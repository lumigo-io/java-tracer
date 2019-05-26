package io.lumigo.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.AwsUtils;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.Span;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private List<Span> httpSpans = new LinkedList<>();
    private Reporter reporter;

    private static final SpansContainer ourInstance = new SpansContainer();

    public static SpansContainer getInstance() {
        return ourInstance;
    }

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

    public List<Span> getAllCollectedSpans() {
        List<Span> spans = new LinkedList<>();
        spans.addAll(httpSpans);
        spans.add(endFunctionSpan);
        return spans;
    }

    public Span getEndSpan() {
        return endFunctionSpan;
    }

    public List<Span> getHttpSpans() {
        return httpSpans;
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
