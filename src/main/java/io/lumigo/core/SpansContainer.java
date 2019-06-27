package io.lumigo.core;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.lambda.runtime.Context;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.parsers.AwsParserFactory;
import io.lumigo.core.utils.AwsUtils;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.HttpSpan;
import io.lumigo.models.Span;
import java.io.*;
import java.util.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.pmw.tinylog.Logger;

public class SpansContainer {

    private static final int MAX_STRING_SIZE = Configuration.getInstance().maxSpanFieldSize();
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
        awsTracerId = env.get(AMZN_TRACE_ID);
        AwsUtils.TriggeredBy triggeredBy = AwsUtils.extractTriggeredByFromEvent(event);
        this.baseSpan =
                Span.builder()
                        .token(Configuration.getInstance().getLumigoToken())
                        .id(context.getAwsRequestId())
                        .started(System.currentTimeMillis())
                        .name(context.getFunctionName())
                        .runtime(env.get(AWS_EXECUTION_ENV))
                        .region(env.get(AWS_REGION))
                        .memoryAllocated(String.valueOf(context.getMemoryLimitInMB()))
                        .requestId(context.getAwsRequestId())
                        .account(AwsUtils.extractAwsAccountFromArn(context.getInvokedFunctionArn()))
                        .maxFinishTime(
                                (context.getRemainingTimeInMillis() > 0)
                                        ? context.getRemainingTimeInMillis()
                                        : MAX_LAMBDA_TIME)
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
                                        .logGroupName(context.getLogGroupName())
                                        .logStreamName(context.getLogStreamName())
                                        .build())
                        .type(FUNCTION_SPAN_TYPE)
                        .readiness(AwsUtils.getFunctionReadiness().toString())
                        .envs(
                                Configuration.getInstance().isLumigoVerboseMode()
                                        ? StringUtils.getMaxSizeString(
                                                JsonUtils.getObjectAsJsonString(env))
                                        : null)
                        .event(
                                Configuration.getInstance().isLumigoVerboseMode()
                                        ? StringUtils.getMaxSizeString(
                                                JsonUtils.getObjectAsJsonString(event))
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
            rttDuration = reporter.reportSpans(startFunctionSpan);
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
                                        ? StringUtils.getMaxSizeString(
                                                JsonUtils.getObjectAsJsonString(response))
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
        reporter.reportSpans(getAllCollectedSpans());
    }

    public Span getStartFunctionSpan() {
        return startFunctionSpan;
    }

    public List<Object> getAllCollectedSpans() {
        List<Object> spans = new LinkedList<>();
        spans.addAll(httpSpans);
        spans.add(endFunctionSpan);
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

    public void addHttpSpan(Long startTime, HttpUriRequest request, HttpResponse response)
            throws Exception {
        HttpSpan httpSpan = createBaseHttpSpan(startTime);
        httpSpan.getInfo()
                .setHttpInfo(
                        HttpSpan.HttpInfo.builder()
                                .host(request.getURI().getHost())
                                .request(
                                        HttpSpan.HttpData.builder()
                                                .headers(extractHeaders(request.getAllHeaders()))
                                                .uri(
                                                        Configuration.getInstance()
                                                                        .isLumigoVerboseMode()
                                                                ? request.getURI().toString()
                                                                : null)
                                                .method(request.getMethod())
                                                .body(extractBodyFromRequest(request))
                                                .build())
                                .response(
                                        HttpSpan.HttpData.builder()
                                                .headers(extractHeaders(response.getAllHeaders()))
                                                .body(extractBodyFromResponse(response))
                                                .statusCode(
                                                        response.getStatusLine().getStatusCode())
                                                .build())
                                .build());
        httpSpans.add(httpSpan);
    }

    public void addHttpSpan(Long startTime, Request<?> request, Response<?> response) {
        HttpSpan httpSpan = createBaseHttpSpan(startTime);
        httpSpan.getInfo()
                .setHttpInfo(
                        HttpSpan.HttpInfo.builder()
                                .host(request.getEndpoint().getHost())
                                .request(
                                        HttpSpan.HttpData.builder()
                                                .headers(extractHeaders(request.getHeaders()))
                                                .uri(
                                                        Configuration.getInstance()
                                                                        .isLumigoVerboseMode()
                                                                ? request.getEndpoint().toString()
                                                                : null)
                                                .method(request.getHttpMethod().name())
                                                .body(extractBodyFromRequest(request))
                                                .build())
                                .response(
                                        HttpSpan.HttpData.builder()
                                                .headers(
                                                        extractHeaders(
                                                                response.getHttpResponse()
                                                                        .getHeaders()))
                                                .body(extractBodyFromResponse(response))
                                                .statusCode(
                                                        response.getHttpResponse().getStatusCode())
                                                .build())
                                .build());
        AwsParserFactory.getParser(request.getServiceName()).parse(httpSpan, request, response);
        httpSpans.add(httpSpan);
    }

    private static String extractHeaders(Header[] headers) {
        return Configuration.getInstance().isLumigoVerboseMode()
                ? StringUtils.getMaxSizeString(
                        JsonUtils.getObjectAsJsonString(convertHeadersToMap(headers)))
                : null;
    }

    private static String extractHeaders(Map<String, String> headers) {
        return Configuration.getInstance().isLumigoVerboseMode()
                ? StringUtils.getMaxSizeString(JsonUtils.getObjectAsJsonString(headers))
                : null;
    }

    protected static Map<String, String> convertHeadersToMap(Header[] headers) {
        Map<String, String> headersMap = new HashMap<>();
        if (headers != null) {
            for (Header header : headers) {
                headersMap.put(header.getName(), header.getValue());
            }
        }
        return headersMap;
    }

    protected static String extractBodyFromRequest(Request<?> request) {
        try {
            if (Configuration.getInstance().isLumigoVerboseMode()) {
                if (request.getContent() != null) {
                    return StringUtils.extractStringForStream(
                            request.getContent(), MAX_STRING_SIZE);
                }
            }
            return null;
        } catch (Exception e) {
            Logger.error(e, "Failed to extract body from request");
            return null;
        }
    }

    protected static String extractBodyFromRequest(HttpUriRequest request) {
        try {
            if (Configuration.getInstance().isLumigoVerboseMode()
                    && request instanceof HttpEntityEnclosingRequestBase) {
                HttpEntity entity = ((HttpEntityEnclosingRequestBase) request).getEntity();
                if (entity != null && entity.getContent() != null) {
                    return StringUtils.extractStringForStream(entity.getContent(), MAX_STRING_SIZE);
                }
            }
            return null;
        } catch (Exception e) {
            Logger.error(e, "Failed to extract body from request");
            return null;
        }
    }

    protected static String extractBodyFromResponse(HttpResponse response) throws IOException {
        return Configuration.getInstance().isLumigoVerboseMode()
                ? StringUtils.extractStringForStream(
                        response.getEntity() != null ? response.getEntity().getContent() : null,
                        MAX_STRING_SIZE)
                : null;
    }

    protected static String extractBodyFromResponse(Response response) {
        return Configuration.getInstance().isLumigoVerboseMode()
                ? StringUtils.getMaxSizeString(
                        response.getAwsResponse() != null
                                ? JsonUtils.getObjectAsJsonString(response.getAwsResponse())
                                : null)
                : null;
    }

    public String getPatchedRoot() {
        return String.format(
                "Root=%s-0000%s-%s%s",
                AwsUtils.extractAwsTraceRoot(awsTracerId),
                StringUtils.randomStringAndNumbers(4),
                AwsUtils.extractAwsTraceTransactionId(awsTracerId),
                AwsUtils.extractAwsTraceSuffix(awsTracerId));
    }
}
