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
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.SecretScrubber;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.HttpSpan;
import io.lumigo.models.Span;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
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

    private Span baseSpan;
    private Span startFunctionSpan;
    private Long rttDuration;
    private Span endFunctionSpan;
    private Reporter reporter;
    private List<HttpSpan> httpSpans = new LinkedList<>();
    private Map<String, String> env;

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
        httpSpans = new LinkedList<>();
    }

    private SpansContainer() {}

    public void init(Map<String, String> env, Reporter reporter, Context context, Object event) {
        this.clear();
        this.reporter = reporter;
        this.env = env;
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
                                        ? JsonUtils.getObjectAsJsonString(
                                                SecretScrubber.getInstance().scrubEnv(env))
                                        : null)
                        .event(
                                Configuration.getInstance().isLumigoVerboseMode()
                                        ? SecretScrubber.getInstance()
                                                .scrubBody(
                                                        JsonUtils.getObjectAsJsonString(
                                                                EventParserFactory.parseEvent(
                                                                        event)),
                                                        env)
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
            rttDuration =
                    reporter.reportSpans(prepareToSend(startFunctionSpan, false), MAX_REQUEST_SIZE);
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

    public Span getStartFunctionSpan() {
        return startFunctionSpan;
    }

    public List<Object> getAllCollectedSpans() {
        List<Object> spans = new LinkedList<>();
        spans.add(endFunctionSpan);
        spans.addAll(httpSpans);
        return spans;
    }

    public Span getEndSpan() {
        return endFunctionSpan;
    }

    public List<HttpSpan> getHttpSpans() {
        return httpSpans;
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
                                                                                SecretScrubber
                                                                                        .getInstance()
                                                                                        .scrubHeaders(
                                                                                                request
                                                                                                        .getAllHeaders(),
                                                                                                this
                                                                                                        .env))))
                                                .uri(
                                                        callIfVerbose(
                                                                () -> request.getURI().toString()))
                                                .method(request.getMethod())
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        SecretScrubber.getInstance()
                                                                                .scrubBody(
                                                                                        extractBodyFromRequest(
                                                                                                request),
                                                                                        this.env)))
                                                .build())
                                .response(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        callIfVerbose(
                                                                () ->
                                                                        extractHeaders(
                                                                                SecretScrubber
                                                                                        .getInstance()
                                                                                        .scrubHeaders(
                                                                                                response
                                                                                                        .getAllHeaders(),
                                                                                                this
                                                                                                        .env))))
                                                .body(
                                                        callIfVerbose(
                                                                () ->
                                                                        SecretScrubber.getInstance()
                                                                                .scrubBody(
                                                                                        extractBodyFromResponse(
                                                                                                response),
                                                                                        this.env)))
                                                .statusCode(
                                                        response.getStatusLine().getStatusCode())
                                                .build())
                                .build());
        httpSpans.add(httpSpan);
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
        httpSpans.add(httpSpan);
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

        httpSpans.add(httpSpan);
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

    private Object prepareToSend(Object span, boolean hasError) {
        return reduceSpanSize(span, hasError);
    }

    private List<Object> prepareToSend(List<Object> spans, boolean hasError) {
        for (Object span : spans) {
            reduceSpanSize(span, hasError);
        }
        return spans;
    }

    public Object reduceSpanSize(Object span, boolean hasError) {
        int maxFieldSize =
                hasError
                        ? Configuration.getInstance().maxSpanFieldSizeWhenError()
                        : Configuration.getInstance().maxSpanFieldSize();
        if (span instanceof Span) {
            Span functionSpan = (Span) span;
            functionSpan.setEnvs(
                    StringUtils.getMaxSizeString(
                            functionSpan.getEnvs(),
                            Configuration.getInstance().maxSpanFieldSize()));
            functionSpan.setReturn_value(
                    StringUtils.getMaxSizeString(functionSpan.getReturn_value(), maxFieldSize));
            functionSpan.setEvent(
                    StringUtils.getMaxSizeString(functionSpan.getEvent(), maxFieldSize));
        } else if (span instanceof HttpSpan) {
            HttpSpan httpSpan = (HttpSpan) span;
            httpSpan.getInfo()
                    .getHttpInfo()
                    .getRequest()
                    .setHeaders(
                            StringUtils.getMaxSizeString(
                                    httpSpan.getInfo().getHttpInfo().getRequest().getHeaders(),
                                    maxFieldSize));
            httpSpan.getInfo()
                    .getHttpInfo()
                    .getRequest()
                    .setBody(
                            StringUtils.getMaxSizeString(
                                    httpSpan.getInfo().getHttpInfo().getRequest().getBody(),
                                    maxFieldSize));
            httpSpan.getInfo()
                    .getHttpInfo()
                    .getResponse()
                    .setHeaders(
                            StringUtils.getMaxSizeString(
                                    httpSpan.getInfo().getHttpInfo().getResponse().getHeaders(),
                                    maxFieldSize));
            httpSpan.getInfo()
                    .getHttpInfo()
                    .getResponse()
                    .setBody(
                            StringUtils.getMaxSizeString(
                                    httpSpan.getInfo().getHttpInfo().getResponse().getBody(),
                                    maxFieldSize));
        }
        return span;
    }
}
