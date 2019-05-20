package io.lumigo.core.utils;

import com.amazonaws.services.lambda.runtime.events.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.lumigo.models.Span;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pmw.tinylog.Logger;

public class AwsUtils {

    public static final String COLD_START_KEY = "COLD_START_KEY";

    /**
     * @param arn an arn of the with the format arn:aws:lambda:{region}:{account}:function:{name}
     * @return The account id, or null if we failed to parse
     */
    public static String extractAwsAccountFromArn(String arn) {
        String[] arnParts = arn.split(":");
        if (arnParts.length < 7) {
            return null;
        }
        return arnParts[4];
    }

    /**
     * @param event an AWS event
     * @return triggered by metadata
     */
    public static String extractTriggeredByFromEvent(Object event) {
        try {
            TriggeredBy triggeredBy = new TriggeredBy();
            Logger.info(
                    "Trying to find triggered by to event from class {}",
                    event != null ? event.getClass().getName() : null);
            if (event == null) {
                return null;
            } else if (event instanceof DynamodbEvent) {
                triggeredBy.setTriggeredBy("dynamodb");
                if (((DynamodbEvent) event).getRecords() != null
                        && ((DynamodbEvent) event).getRecords().size() > 0) {
                    triggeredBy.setArn(
                            ((DynamodbEvent) event).getRecords().get(0).getEventSourceARN());
                }
            } else if (event instanceof KinesisEvent) {
                triggeredBy.setTriggeredBy("kinesis");
                if (((KinesisEvent) event).getRecords() != null
                        && ((KinesisEvent) event).getRecords().size() > 0) {
                    triggeredBy.setArn(
                            ((KinesisEvent) event).getRecords().get(0).getEventSourceARN());
                }
            } else if (event instanceof KinesisFirehoseEvent) {
                triggeredBy.setTriggeredBy("kinesis");
                triggeredBy.setArn(((KinesisFirehoseEvent) event).getDeliveryStreamArn());
            } else if (event instanceof KinesisAnalyticsFirehoseInputPreprocessingEvent) {
                triggeredBy.setTriggeredBy("kinesis");
                triggeredBy.setArn(
                        ((KinesisAnalyticsFirehoseInputPreprocessingEvent) event).getStreamArn());
            } else if (event instanceof KinesisAnalyticsStreamsInputPreprocessingEvent) {
                triggeredBy.setTriggeredBy("kinesis");
                triggeredBy.setArn(
                        ((KinesisAnalyticsStreamsInputPreprocessingEvent) event).getStreamArn());
            } else if (event instanceof S3Event) {
                triggeredBy.setTriggeredBy("s3");
                if (((S3Event) event).getRecords() != null
                        && ((S3Event) event).getRecords().size() > 0) {
                    triggeredBy.setArn(
                            ((S3Event) event).getRecords().get(0).getS3().getBucket().getArn());
                }
            } else if (event instanceof SNSEvent) {
                triggeredBy.setTriggeredBy("sns");
                if (((SNSEvent) event).getRecords() != null
                        && ((SNSEvent) event).getRecords().size() > 0) {
                    triggeredBy.setArn(
                            ((SNSEvent) event).getRecords().get(0).getSNS().getTopicArn());
                }
            } else if (event instanceof SQSEvent) {
                triggeredBy.setTriggeredBy("sqs");
                if (((SQSEvent) event).getRecords() != null
                        && ((SQSEvent) event).getRecords().size() > 0) {
                    triggeredBy.setArn(((SQSEvent) event).getRecords().get(0).getEventSourceArn());
                }
            } else if (event instanceof APIGatewayProxyRequestEvent) {
                triggeredBy.setTriggeredBy("apigw");
                triggeredBy.setHttpMethod(((APIGatewayProxyRequestEvent) event).getHttpMethod());
                triggeredBy.setResource(((APIGatewayProxyRequestEvent) event).getResource());
                if (((APIGatewayProxyRequestEvent) event).getHeaders() != null) {
                    triggeredBy.setApi(
                            ((APIGatewayProxyRequestEvent) event)
                                    .getHeaders()
                                    .getOrDefault("Host", "unknown.unknown.unknown"));
                }
                if (((APIGatewayProxyRequestEvent) event).getRequestContext() != null
                        && ((APIGatewayProxyRequestEvent) event).getRequestContext().getStage()
                                != null) {
                    triggeredBy.setStage(
                            ((APIGatewayProxyRequestEvent) event).getRequestContext().getStage());
                }
            } else if (event instanceof CloudWatchLogsEvent) {
                triggeredBy.setTriggeredBy("cloudwatch");
            } else if (event instanceof ScheduledEvent) {
                triggeredBy.setTriggeredBy("cloudwatch");
                if (((ScheduledEvent) event).getResources() != null
                        && ((ScheduledEvent) event).getResources().size() > 0) {
                    triggeredBy.setArn(((ScheduledEvent) event).getResources().get(0));
                }
            } else if (event instanceof CloudFrontEvent) {
                triggeredBy.setTriggeredBy("cloudfront");
            } else if (event instanceof ConfigEvent) {
                triggeredBy.setTriggeredBy("config");
            } else if (event instanceof CodeCommitEvent) {
                triggeredBy.setTriggeredBy("codecommit");
            } else if (event instanceof LexEvent) {
                triggeredBy.setTriggeredBy("lex");
            } else if (event instanceof CognitoEvent) {
                triggeredBy.setTriggeredBy("cognito");
            } else {
                Logger.error(
                        "Failed to found relevant triggered by found for event {} ",
                        event.getClass().getName());
                return null;
            }

            Logger.info("Found triggered by handler fo event {}", event.getClass().getName());
            return JsonUtils.getObjectAsJsonString(triggeredBy);

        } catch (RuntimeException | JsonProcessingException e) {
            Logger.error(e, "Failed to extract triggerBy data");
            return null;
        }
    }

    /**
     * @param amznTraceId with the form `Root=1-2-3;Another=456;Bla=789`
     * @return The value of the root phrase
     */
    public static String extractAwsTraceRoot(String amznTraceId) {
        Matcher matcher = Pattern.compile("([^;]+)=([^;]*)").matcher(amznTraceId);
        while (matcher.find()) {
            if (matcher.group(1).equals("Root")) {
                return matcher.group(2);
            }
        }
        return null;
    }

    /**
     * @param amznTraceId with the form `Root=1-2-3;Another=456;Bla=789`
     * @return The the third part in the root phrase
     */
    public static String extractAwsTraceTransactionId(String amznTraceId) {
        String root = extractAwsTraceRoot(amznTraceId);
        if (root == null) {
            return null;
        }
        String[] rootParts = root.split("-");
        if (rootParts.length < 3) {
            return null;
        }
        return rootParts[2];
    }

    public static String extractAwsTraceSuffix(String amznTraceId) {
        if (!amznTraceId.contains(";")) {
            return amznTraceId;
        }
        return amznTraceId.substring(amznTraceId.indexOf(";"));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @NoArgsConstructor
    @Data
    private static class TriggeredBy {
        private String triggeredBy;
        private String arn;
        private String httpMethod;
        private String resource;
        private String api;
        private String stage;
    }

    public static Span.READINESS getFunctionReadiness() {
        if (System.getProperty(COLD_START_KEY) != null) {
            return Span.READINESS.WARM;
        } else {
            System.setProperty(COLD_START_KEY, "false");
            return Span.READINESS.COLD;
        }
    }
}
