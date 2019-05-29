package io.lumigo.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.AwsUtils;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.HttpSpan;
import io.lumigo.models.Span;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.pmw.tinylog.Logger;

public class SpansContainer {

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

    public void init(Map<String, String> env, Reporter reporter, Context context, Object event)
            throws JsonProcessingException {
        this.clear();
        this.reporter = reporter;
        awsTracerId = env.get(AMZN_TRACE_ID);
        this.baseSpan =
                Span.builder()
                        .token(Configuration.getInstance().getLumigoToken())
                        .id(context.getAwsRequestId())
                        .started(System.currentTimeMillis())
                        .name(context.getFunctionName())
                        .runtime(env.get(AWS_EXECUTION_ENV))
                        .region(env.get(AWS_REGION))
                        .memoryAllocated(context.getMemoryLimitInMB())
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
                                        .triggeredBy(AwsUtils.extractTriggeredByFromEvent(event))
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

    public void addHttpSpan(Long startTime, HttpUriRequest request, HttpResponse response)
            throws Exception {
        httpSpans.add(
                HttpSpan.builder()
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
                                                                baseSpan.getInfo()
                                                                        .getTracer()
                                                                        .getVersion())
                                                        .build())
                                        .traceId(
                                                HttpSpan.TraceId.builder()
                                                        .root(
                                                                baseSpan.getInfo()
                                                                        .getTraceId()
                                                                        .getRoot())
                                                        .build())
                                        .httpInfo(
                                                HttpSpan.HttpInfo.builder()
                                                        .host(request.getURI().getHost())
                                                        .request(
                                                                HttpSpan.HttpData.builder()
                                                                        .headers(
                                                                                Configuration
                                                                                                .getInstance()
                                                                                                .isLumigoVerboseMode()
                                                                                        ? StringUtils
                                                                                                .getMaxSizeString(
                                                                                                        JsonUtils
                                                                                                                .getObjectAsJsonString(
                                                                                                                        convertHeadersToMap(
                                                                                                                                request
                                                                                                                                        .getAllHeaders())))
                                                                                        : null)
                                                                        .uri(
                                                                                Configuration
                                                                                                .getInstance()
                                                                                                .isLumigoVerboseMode()
                                                                                        ? request.getURI()
                                                                                                .toString()
                                                                                        : null)
                                                                        .method(request.getMethod())
                                                                        .body(
                                                                                Configuration
                                                                                                .getInstance()
                                                                                                .isLumigoVerboseMode()
                                                                                        ? extractBodyFromRequest(
                                                                                                request)
                                                                                        : null)
                                                                        .build())
                                                        .response(
                                                                HttpSpan.HttpData.builder()
                                                                        .headers(
                                                                                Configuration
                                                                                                .getInstance()
                                                                                                .isLumigoVerboseMode()
                                                                                        ? StringUtils
                                                                                                .getMaxSizeString(
                                                                                                        JsonUtils
                                                                                                                .getObjectAsJsonString(
                                                                                                                        convertHeadersToMap(
                                                                                                                                response
                                                                                                                                        .getAllHeaders())))
                                                                                        : null)
                                                                        .body(
                                                                                Configuration
                                                                                                .getInstance()
                                                                                                .isLumigoVerboseMode()
                                                                                        ? extractStringFromInputStream(
                                                                                                response
                                                                                                                        .getEntity()
                                                                                                                != null
                                                                                                        ? response.getEntity()
                                                                                                                .getContent()
                                                                                                        : null)
                                                                                        : null)
                                                                        .statusCode(
                                                                                response.getStatusLine()
                                                                                        .getStatusCode())
                                                                        .build())
                                                        .build())
                                        .build())
                        .build());
    }

    protected Map<String, String> convertHeadersToMap(Header[] headers) {
        Map<String, String> headersMap = new HashMap<>();
        if (headers != null) {
            for (Header header : headers) {
                headersMap.put(header.getName(), header.getValue());
            }
        }
        return headersMap;
    }

    protected static String extractBodyFromRequest(HttpUriRequest request) {
        try {
            if (request instanceof HttpEntityEnclosingRequestBase) {
                HttpEntity entity = ((HttpEntityEnclosingRequestBase) request).getEntity();
                if (entity != null && entity.getContent() != null) {
                    return extractStringFromInputStream(entity.getContent());
                }
            }
            return null;
        } catch (Exception e) {
            Logger.error(e, "Failed to extract body from request");
            return null;
        }
    }

    protected static String extractStringFromInputStream(InputStream inputStream) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            if (inputStream != null && inputStream.markSupported()) {
                Logger.info("Stream reset supported, response body will be extracted");
                IOUtils.copy(inputStream, byteArrayOutputStream);
                inputStream.reset();
                return StringUtils.getMaxSizeString(
                        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset()));
            } else {
                Logger.info("Stream reset is not supported, response body will not be extracted");
                return null;
            }
        } catch (Throwable e) {
            Logger.error(e, "Failed to extract body from request");
            return null;
        }
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
