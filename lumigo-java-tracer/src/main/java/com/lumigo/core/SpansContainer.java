package com.lumigo.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.lumigo.core.configuration.LumigoConfiguration;
import com.lumigo.core.utils.AwsUtils;
import com.lumigo.models.Span;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SpansContainer {

    private static final int MAX_LAMBDA_TIME = 15 * 60 * 1000;
    private static final String AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV";
    private static final String AWS_REGION = "AWS_REGION";
    private static final String AMZN_TRACE_ID = "_X_AMZN_TRACE_ID";

    private Span baseSpan;
    private Span startFunctionSpan;
    private Span endFunctionSpan;
    private List<Span> httpSpans = new LinkedList<>();

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

    private SpansContainer() {
    }


    public void init(Map<String, String> env, Context context, Object event) {
        this.baseSpan = Span.builder().
                id(context.getAwsRequestId()).
                started(System.currentTimeMillis()).
                name(context.getFunctionName()).
                runtime(env.get(AWS_EXECUTION_ENV)).
                region(env.get(AWS_REGION)).
                memoryAllocated(context.getMemoryLimitInMB()).
                logGroupName(context.getLogGroupName()).
                logStreamName(context.getLogStreamName()).
                requestId(context.getAwsRequestId()).
                account(AwsUtils.extractAwsAccountFromArn(context.getInvokedFunctionArn())).
                triggerBy(AwsUtils.extractTriggeredByFromEvent(event)).
                maxFinishTime((context.getRemainingTimeInMillis() > 0) ? context.getRemainingTimeInMillis() : MAX_LAMBDA_TIME).
                traceRoot(AwsUtils.extractAwsTraceRoot(env.get(AMZN_TRACE_ID))).
                traceIdSuffix(AwsUtils.extractAwsTraceSuffix(env.get(AMZN_TRACE_ID))).
                transactionId(AwsUtils.extractAwsTraceTransactionId(env.get(AMZN_TRACE_ID))).
                info(Span.Info.builder().
                        tracer(Span.Tracer.builder().
                                version(LumigoConfiguration.getInstance().getLumigoTracerVersion()).
                                build()).
                        build()).
                build();
    }

    public void start() {
        this.startFunctionSpan = this.baseSpan.toBuilder().
                id(this.baseSpan.getId() + "_started").
                ended(this.baseSpan.getStarted()).
                build();

    }

    public void addHttpSpan() {
        //TODO
    }

    public void addException(Throwable e) {
        this.startFunctionSpan = this.baseSpan.toBuilder().
                ended(System.currentTimeMillis()).
                error(Span.Error.builder().message(e.getMessage()).
                        type(e.getClass().getName()).
                        stacktrace(ExceptionUtils.getStackTrace(e)).
                        build()).
                build();
    }

    public void end() {
        this.startFunctionSpan = this.baseSpan.toBuilder().
                id(this.baseSpan.getId()).
                ended(this.baseSpan.getStarted()).
                build();

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

}
