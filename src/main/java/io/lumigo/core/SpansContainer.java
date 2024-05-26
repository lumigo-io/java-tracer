package io.lumigo.core;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.lambda.runtime.Context;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.parsers.event.EventParserFactory;
import io.lumigo.core.parsers.v1.AwsSdkV1ParserFactory;
import io.lumigo.core.parsers.v2.AwsSdkV2ParserFactory;
import io.lumigo.core.utils.AwsUtils;
import io.lumigo.core.utils.EnvUtil;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.SecretScrubber;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.*;
import io.lumigo.models.HttpSpan;
import io.lumigo.models.Span;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import lombok.Getter;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.internals.ProducerMetadata;
import org.apache.kafka.common.serialization.Serializer;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;

public class SpansContainer {

    private static final int MAX_STRING_SIZE = Configuration.getInstance().maxSpanFieldSize();
    private static final int MAX_REQUEST_SIZE = Configuration.getInstance().maxRequestSize();
    private static final int MAX_LAMBDA_TIME = 15 * 60 * 1000;
    private static final String AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV";
    private static final String AWS_REGION = "AWS_REGION";
    private static final String AMZN_TRACE_ID = "_X_AMZN_TRACE_ID";
    private static final String FUNCTION_SPAN_TYPE = "function";
    private static final String HTTP_SPAN_TYPE = "http";
    public static final String KAFKA_SPAN_TYPE = "kafka";

    private Span baseSpan;
    @Getter private Span startFunctionSpan;
    private Long rttDuration;
    private Span endFunctionSpan;
    private Reporter reporter;
    private SecretScrubber secretScrubber = new SecretScrubber(new EnvUtil().getEnv());
    @Getter private List<BaseSpan> spans = new LinkedList<>();

    private static final SpansContainer ourInstance = new SpansContainer();

    public static SpansContainer getInstance() {
        return ourInstance;
    }

    private String awsTracerId;

    public void clear() {
        baseSpan = null;
        startFunctionSpan = null;
        rttDuration = null;
        endFunctionSpan = null;
        reporter = null;
        spans = new LinkedList<>();
    }

    private SpansContainer() {}

    public void init(Map<String, String> env, Reporter reporter, Context context, Object event) {
        this.clear();
        this.reporter = reporter;
        this.secretScrubber = new SecretScrubber(new EnvUtil().getEnv());

        int javaVersion = AwsUtils.parseJavaVersion(System.getProperty("java.version"));
        if (javaVersion > 11) {
            awsTracerId = System.getProperty("com.amazonaws.xray.traceHeader");
        } else {
            awsTracerId = env.get(AMZN_TRACE_ID);
        }
        Logger.debug("awsTracerId {}", awsTracerId);

        AwsUtils.TriggeredBy triggeredBy = AwsUtils.extractTriggeredByFromEvent(event);

        long startTime = System.currentTimeMillis();
        this.baseSpan =
                Span.builder()
                        .token(Configuration.getInstance().getLumigoToken())
                        .id(context.getAwsRequestId())
                        .started(startTime)
                        .name(context.getFunctionName())
                        .runtime(env.get(AWS_EXECUTION_ENV))
                        .region(env.get(AWS_REGION))
                        .memoryAllocated(String.valueOf(context.getMemoryLimitInMB()))
                        .requestId(context.getAwsRequestId())
                        .account(AwsUtils.extractAwsAccountFromArn(context.getInvokedFunctionArn()))
                        .maxFinishTime(
                                startTime
                                        + ((context.getRemainingTimeInMillis() > 0)
                                                ? context.getRemainingTimeInMillis()
                                                : MAX_LAMBDA_TIME))
                        .transactionId(AwsUtils.extractAwsTraceTransactionId(awsTracerId))
                        .info(
                                Span.Info.builder()
                                        .tracer(
                                                Span.Tracer.builder()
                                                        .version(
                                                                Configuration.getInstance()
                                                                        .getLumigoTracerVersion())
                                                        .build())
                                        .traceId(
                                                Span.TraceId.builder()
                                                        .root(
                                                                AwsUtils.extractAwsTraceRoot(
                                                                        awsTracerId))
                                                        .build())
                                        .triggeredBy(
                                                triggeredBy != null
                                                        ? triggeredBy.getTriggeredBy()
                                                        : null)
                                        .api(triggeredBy != null ? triggeredBy.getApi() : null)
                                        .arn(triggeredBy != null ? triggeredBy.getArn() : null)
                                        .httpMethod(
                                                triggeredBy != null
                                                        ? triggeredBy.getHttpMethod()
                                                        : null)
                                        .resource(
                                                triggeredBy != null
                                                        ? triggeredBy.getResource()
                                                        : null)
                                        .stage(triggeredBy != null ? triggeredBy.getStage() : null)
                                        .messageId(
                                                triggeredBy != null
                                                        ? triggeredBy.getMessageId()
                                                        : null)
                                        .messageIds(
                                                triggeredBy != null
                                                        ? triggeredBy.getMessageIds()
                                                        : null)
                                        .approxEventCreationTime(
                                                triggeredBy != null
                                                        ? triggeredBy.getApproxEventCreationTime()
                                                        : 0)
                                        .logGroupName(context.getLogGroupName())
                                        .logStreamName(context.getLogStreamName())
                                        .build())
                        .type(FUNCTION_SPAN_TYPE)
                        .readiness(AwsUtils.getFunctionReadiness().toString())
                        .envs(
                                Configuration.getInstance().isLumigoVerboseMode()
                                        ? JsonUtils.getObjectAsJsonString(env)
                                        : null)
                        .event(
                                Configuration.getInstance().isLumigoVerboseMode()
                                        ? JsonUtils.getObjectAsJsonString(
                                                EventParserFactory.parseEvent(event))
                                        : null)
                        .build();
    }

    public void start() {
        this.startFunctionSpan =
                this.baseSpan
                        .toBuilder()
                        .id(this.baseSpan.getId() + "_started")
                        .ended(this.baseSpan.getStarted())
                        .build();

        try {
            rttDuration = reporter.reportSpans(prepareToSend(startFunctionSpan), MAX_REQUEST_SIZE);
        } catch (Throwable e) {
            Logger.error(e, "Failed to send start span");
        }
    }

    public void end(Object response) throws IOException {
        end(
                this.baseSpan
                        .toBuilder()
                        .return_value(
                                Configuration.getInstance().isLumigoVerboseMode()
                                        ? JsonUtils.getObjectAsJsonString(response)
                                        : null)
                        .build());
    }

    public void endWithException(Throwable e) throws IOException {
        end(
                this.baseSpan
                        .toBuilder()
                        .error(
                                Span.Error.builder()
                                        .message(e.getMessage())
                                        .type(e.getClass().getName())
                                        .stacktrace(getStackTrace(e))
                                        .build())
                        .build());
    }

    public void end() throws IOException {
        end(this.baseSpan);
    }

    private void end(Span endFunctionSpan) throws IOException {
        this.endFunctionSpan =
                endFunctionSpan
                        .toBuilder()
                        .reporter_rtt(rttDuration)
                        .ended(System.currentTimeMillis())
                        .id(this.baseSpan.getId())
                        .build();
        reporter.reportSpans(
                prepareToSend(getAllCollectedSpans(), endFunctionSpan.getError() != null),
                MAX_REQUEST_SIZE);
    }

    public List<BaseSpan> getAllCollectedSpans() {
        List<BaseSpan> spans = new LinkedList<>();
        spans.add(endFunctionSpan);
        spans.addAll(this.spans);
        return spans;
    }

    public Span getEndSpan() {
        return endFunctionSpan;
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    private HttpSpan createBaseHttpSpan(Long startTime) {
        return HttpSpan.builder()
                .id(UUID.randomUUID().toString())
                .started(startTime)
                .ended(System.currentTimeMillis())
                .transactionId(baseSpan.getTransactionId())
                .account(baseSpan.getAccount())
                .region(baseSpan.getRegion())
                .token(baseSpan.getToken())
                .type(HTTP_SPAN_TYPE)
                .parentId(baseSpan.getRequestId())
                .info(
                        HttpSpan.Info.builder()
                                .tracer(
                                        HttpSpan.Tracer.builder()
                                                .version(
                                                        baseSpan.getInfo().getTracer().getVersion())
                                                .build())
                                .traceId(
                                        HttpSpan.TraceId.builder()
                                                .root(baseSpan.getInfo().getTraceId().getRoot())
                                                .build())
                                .build())
                .build();
    }

    public void addHttpSpan(Long startTime, HttpUriRequest request, HttpResponse response) {
        HttpSpan httpSpan = createBaseHttpSpan(startTime);
        httpSpan.getInfo()
                .setHttpInfo(
                        HttpSpan.HttpInfo.builder()
                                .host(request.getURI().getHost())
                                .request(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractHeaders(
                                                                                request
                                                                                        .getAllHeaders())))
                                                .uri(
                                                        callIfVerbose(
                                                                () -> request.getURI().toString()))
                                                .method(request.getMethod())
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractBodyFromRequest(
                                                                                request)))
                                                .build())
                                .response(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractHeaders(
                                                                                response
                                                                                        .getAllHeaders())))
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractBodyFromResponse(
                                                                                response)))
                                                .statusCode(
                                                        response.getStatusLine().getStatusCode())
                                                .build())
                                .build());
        this.spans.add(httpSpan);
    }

    public void addHttpSpan(Long startTime, Request<?> request, Response<?> response) {
        HttpSpan httpSpan = createBaseHttpSpan(startTime);
        String spanId = null;
        for (Map.Entry<String, String> header :
                response.getHttpResponse().getHeaders().entrySet()) {
            if ("x-amzn-requestid".equalsIgnoreCase(header.getKey())
                    || "x-amz-requestid".equalsIgnoreCase(header.getKey())) {
                spanId = header.getValue();
            }
        }
        if (spanId != null) {
            httpSpan.setId(spanId);
        }
        httpSpan.getInfo()
                .setHttpInfo(
                        HttpSpan.HttpInfo.builder()
                                .host(request.getEndpoint().getHost())
                                .request(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractHeaders(
                                                                                request
                                                                                        .getHeaders())))
                                                .uri(
                                                        callIfVerbose(
                                                                () ->
                                                                        request.getEndpoint()
                                                                                .toString()))
                                                .method(request.getHttpMethod().name())
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractBodyFromRequest(
                                                                                request)))
                                                .build())
                                .response(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractHeaders(
                                                                                response.getHttpResponse()
                                                                                        .getHeaders())))
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractBodyFromResponse(
                                                                                response)))
                                                .statusCode(
                                                        response.getHttpResponse().getStatusCode())
                                                .build())
                                .build());
        AwsSdkV1ParserFactory.getParser(request.getServiceName())
                .safeParse(httpSpan, request, response);
        this.spans.add(httpSpan);
    }

    public void addHttpSpan(
            Long startTime,
            final software.amazon.awssdk.core.interceptor.Context.AfterExecution context,
            final ExecutionAttributes executionAttributes) {
        HttpSpan httpSpan = createBaseHttpSpan(startTime);
        String spanId = null;
        for (Map.Entry<String, List<String>> header : context.httpResponse().headers().entrySet()) {
            if ("x-amzn-requestid".equalsIgnoreCase(header.getKey())
                    || "x-amz-requestid".equalsIgnoreCase(header.getKey())) {
                spanId = header.getValue().get(0);
            }
        }
        if (spanId != null) {
            httpSpan.setId(spanId);
        }
        httpSpan.getInfo()
                .setHttpInfo(
                        HttpSpan.HttpInfo.builder()
                                .host(context.httpRequest().getUri().getHost())
                                .request(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractHeadersV2(
                                                                                context.httpRequest()
                                                                                        .headers())))
                                                .uri(
                                                        callIfVerbose(
                                                                () ->
                                                                        context.httpRequest()
                                                                                .getUri()
                                                                                .toString()))
                                                .method(context.httpRequest().method().name())
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractBodyFromRequest(
                                                                                context
                                                                                        .requestBody())))
                                                .build())
                                .response(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractHeadersV2(
                                                                                context.httpResponse()
                                                                                        .headers())))
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractBodyFromResponse(
                                                                                context
                                                                                        .response())))
                                                .statusCode(context.httpResponse().statusCode())
                                                .build())
                                .build());

        Logger.debug(
                "Trying to extract aws custom properties for service: "
                        + executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME));
        AwsSdkV2ParserFactory.getParser(
                        executionAttributes.getAttribute(SdkExecutionAttribute.SERVICE_NAME))
                .safeParse(httpSpan, context);

        this.spans.add(httpSpan);
    }

    public <K, V> void addKafkaProduceSpan(
            Long startTime,
            Serializer<K> keySerializer,
            Serializer<V> valueSerializer,
            ProducerMetadata producerMetadata,
            ProducerRecord<K, V> record,
            RecordMetadata recordMetadata,
            Exception exception) {
        this.spans.add(
                KafkaSpanFactory.createProduce(
                        this.baseSpan,
                        startTime,
                        keySerializer,
                        valueSerializer,
                        producerMetadata,
                        record,
                        recordMetadata,
                        exception));
    }

    public void addKafkaConsumeSpan(
            Long startTime,
            KafkaConsumer<?, ?> consumer,
            ConsumerMetadata consumerMetadata,
            ConsumerRecords<?, ?> consumerRecords) {
        this.spans.add(
                KafkaSpanFactory.createConsume(
                        this.baseSpan, startTime, consumer, consumerMetadata, consumerRecords));
    }

    private static String extractHeaders(Map<String, String> headers) {
        return JsonUtils.getObjectAsJsonString(headers);
    }

    private static String extractHeadersV2(Map<String, List<String>> headers) {
        return JsonUtils.getObjectAsJsonString(headers);
    }

    private static String extractHeaders(Header[] headers) {
        Map<String, String> headersMap = new HashMap<>();
        if (headers != null) {
            for (Header header : headers) {
                headersMap.put(header.getName(), header.getValue());
            }
        }
        return extractHeaders(headersMap);
    }

    protected static String extractBodyFromRequest(Request<?> request) {
        return extractBodyFromStream(request.getContent());
    }

    protected static String extractBodyFromRequest(Optional<RequestBody> request) {
        return request.map(
                        requestBody ->
                                extractBodyFromStream(
                                        requestBody.contentStreamProvider().newStream()))
                .orElse(null);
    }

    protected static String extractBodyFromRequest(HttpUriRequest request) throws Exception {
        if (request instanceof HttpEntityEnclosingRequestBase) {
            HttpEntity entity = ((HttpEntityEnclosingRequestBase) request).getEntity();
            if (entity != null) {
                return extractBodyFromStream(entity.getContent());
            }
        }
        return null;
    }

    protected static String extractBodyFromResponse(HttpResponse response) throws IOException {
        return response.getEntity() != null
                ? extractBodyFromStream(response.getEntity().getContent())
                : null;
    }

    protected static String extractBodyFromResponse(Response response) {
        return response.getAwsResponse() != null
                ? JsonUtils.getObjectAsJsonString(response.getAwsResponse())
                : null;
    }

    protected static String extractBodyFromResponse(SdkResponse response) {
        if (response instanceof AwsResponse) {
            return JsonUtils.getObjectAsJsonString(response.toBuilder());
        }
        return null;
    }

    protected static String extractBodyFromStream(InputStream stream) {
        return StringUtils.extractStringForStream(stream, MAX_STRING_SIZE);
    }

    public String getPatchedRoot() {
        return String.format(
                "Root=%s-0000%s-%s%s",
                AwsUtils.extractAwsTraceRoot(awsTracerId),
                StringUtils.randomStringAndNumbers(4),
                AwsUtils.extractAwsTraceTransactionId(awsTracerId),
                AwsUtils.extractAwsTraceSuffix(awsTracerId));
    }

    protected static <T> T callIfVerbose(Callable<T> method) {
        if (!Configuration.getInstance().isLumigoVerboseMode()) {
            return null;
        }
        try {
            return method.call();
        } catch (Exception e) {
            Logger.error(e, "Failed to call method");
            return null;
        }
    }

    private BaseSpan prepareToSend(BaseSpan span) {
        return reduceSpanSize(span.scrub(secretScrubber), false);
    }

    private List<BaseSpan> prepareToSend(List<BaseSpan> spans, boolean hasError) {
        for (BaseSpan span : spans) {
            reduceSpanSize(span.scrub(secretScrubber), hasError);
        }
        return spans;
    }

    public BaseSpan reduceSpanSize(BaseSpan span, boolean hasError) {
        int maxFieldSize =
                hasError
                        ? Configuration.getInstance().maxSpanFieldSizeWhenError()
                        : Configuration.getInstance().maxSpanFieldSize();
        return span.reduceSize(maxFieldSize);
    }
}
