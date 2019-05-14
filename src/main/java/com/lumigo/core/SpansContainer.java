package com.lumigo.core;

import com.amazonaws.services.lambda.runtime.Context;
import com.lumigo.core.configuration.LumigoConfiguration;
import com.lumigo.core.utils.AwsUtils;
import com.lumigo.core.utils.JsonUtils;
import com.lumigo.core.utils.StringUtils;
import com.lumigo.models.Span;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.pmw.tinylog.Logger;

public class SpansContainer {

  private static final int MAX_LAMBDA_TIME = 15 * 60 * 1000;
  private static final String AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV";
  private static final String AWS_REGION = "AWS_REGION";
  private static final String AMZN_TRACE_ID = "_X_AMZN_TRACE_ID";
  private static final String FUNCTION_SPAN_TYPE = "function";
  private static final String HTTP_SPAN_TYPE = "http";
  private static final String WARM_READINESS = "warm";

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

  private SpansContainer() {}

  public void init(Map<String, String> env, Context context, Object event) {
    try {
      String awsTracerId = env.get(AMZN_TRACE_ID);
      this.baseSpan =
          Span.builder()
              .token(LumigoConfiguration.getInstance().getLumigoToken())
              .id(context.getAwsRequestId())
              .started(System.currentTimeMillis())
              .name(context.getFunctionName())
              .runtime(env.get(AWS_EXECUTION_ENV))
              .region(env.get(AWS_REGION))
              .memoryAllocated(context.getMemoryLimitInMB())
              .logGroupName(context.getLogGroupName())
              .logStreamName(context.getLogStreamName())
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
                              .version(LumigoConfiguration.getInstance().getLumigoTracerVersion())
                              .build())
                      .traceId(
                          Span.TraceId.builder()
                              .root(AwsUtils.extractAwsTraceRoot(awsTracerId))
                              .build())
                      .build())
              .type(FUNCTION_SPAN_TYPE)
              .readiness(WARM_READINESS)
              .build();
    } catch (Exception e) {
      Logger.error(e, "Failed to create base span");
    }
  }

  public void start() {
    try {
      this.startFunctionSpan =
          this.baseSpan
              .toBuilder()
              .id(this.baseSpan.getId() + "_started")
              .ended(this.baseSpan.getStarted())
              .build();
    } catch (Exception e) {
      Logger.error(e, "Failed to create start span");
    }
  }

  public void end(Object response) {
    try {
      this.endFunctionSpan =
          this.baseSpan
              .toBuilder()
              .id(this.baseSpan.getId())
              .ended(this.baseSpan.getStarted())
              .return_value(StringUtils.getMaxSizeString(JsonUtils.getObjectAsJsonString(response)))
              .build();
    } catch (Exception e) {
      Logger.error(e, "Failed to create end with response span");
    }
  }

  public void endWithException(Throwable e) {
    try {
      this.endFunctionSpan =
          this.baseSpan
              .toBuilder()
              .ended(System.currentTimeMillis())
              .error(
                  Span.Error.builder()
                      .message(e.getMessage())
                      .type(e.getClass().getName())
                      .stacktrace(ExceptionUtils.getStackTrace(e))
                      .build())
              .build();
    } catch (Exception ex) {
      Logger.error(ex, "Failed to create endWithException span");
    }
  }

  public void end() {
    try {
      this.endFunctionSpan =
          this.baseSpan
              .toBuilder()
              .id(this.baseSpan.getId())
              .ended(this.baseSpan.getStarted())
              .build();
    } catch (Exception ex) {
      Logger.error(ex, "Failed to create end span");
    }
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
}
