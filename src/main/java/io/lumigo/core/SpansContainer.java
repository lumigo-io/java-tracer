package io.lumigo.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.AwsUtils;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.HttpSpan;
import io.lumigo.models.Span;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;

public class SpansContainer {

    private static final int MAX_LAMBDA_TIME = 15 * 60 * 1000;
    private static final String AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV";
    private static final String AWS_REGION = "AWS_REGION";
    private static final String AMZN_TRACE_ID = "_X_AMZN_TRACE_ID";
    private static final String FUNCTION_SPAN_TYPE = "function";
    private static final String HTTP_SPAN_TYPE = "http";

    private Span baseSpan;
    private Span startFunctionSpan;
    private Span endFunctionSpan;
    private List<HttpSpan> httpSpans = new LinkedList<>();

    private static final SpansContainer ourInstance = new SpansContainer();

    public static SpansContainer getInstance() {
        return ourInstance;
    }

    public void clear() {
        baseSpan = null;
        startFunctionSpan = null;
        endFunctionSpan = null;
        httpSpans = new LinkedList<>();
    }

    private SpansContainer() {}

    public void init(Map<String, String> env, Context context, Object event)
            throws JsonProcessingException {
        this.clear();
        String awsTracerId = env.get(AMZN_TRACE_ID);
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
    }

    public void end(Object response) throws JsonProcessingException {

        this.endFunctionSpan =
                this.baseSpan
                        .toBuilder()
                        .id(this.baseSpan.getId())
                        .ended(System.currentTimeMillis())
                        .return_value(
                                Configuration.getInstance().isLumigoVerboseMode()
                                        ? StringUtils.getMaxSizeString(
                                                JsonUtils.getObjectAsJsonString(response))
                                        : null)
                        .build();
    }

    public void endWithException(Throwable e) {

        this.endFunctionSpan =
                this.baseSpan
                        .toBuilder()
                        .ended(System.currentTimeMillis())
                        .error(
                                Span.Error.builder()
                                        .message(e.getMessage())
                                        .type(e.getClass().getName())
                                        .stacktrace(getStackTrace(e))
                                        .build())
                        .build();
    }

    public void end() {

        this.endFunctionSpan =
                this.baseSpan
                        .toBuilder()
                        .id(this.baseSpan.getId())
                        .ended(System.currentTimeMillis())
                        .build();
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

    public void addHttpSpan(URI uri, Map<String, String> headers) throws Exception {
        httpSpans.add(
                HttpSpan.builder()
                        .id(baseSpan.getId())
                        .started(System.currentTimeMillis())
                        .transactionId(baseSpan.getTransactionId())
                        .account(baseSpan.getAccount())
                        .region(baseSpan.getRegion())
                        .token(baseSpan.getToken())
                        .type(HTTP_SPAN_TYPE)
                        .info(
                                HttpSpan.Info.builder()
                                        .httpInfo(
                                                HttpSpan.HttpInfo.builder()
                                                        .host(uri.getHost())
                                                        .request(
                                                                HttpSpan.HttpData.builder()
                                                                        .headers(
                                                                                StringUtils
                                                                                        .getMaxSizeString(
                                                                                                JsonUtils
                                                                                                        .getObjectAsJsonString(
                                                                                                                headers)))
                                                                        .build())
                                                        .build())
                                        .build())
                        .build());
    }
}
