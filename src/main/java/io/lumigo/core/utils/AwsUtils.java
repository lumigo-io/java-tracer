package io.lumigo.core.utils;

import com.amazonaws.services.lambda.runtime.events.*;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.models.KafkaSpan;
import io.lumigo.models.Span;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pmw.tinylog.Logger;

public class AwsUtils {

    public static final String COLD_START_KEY = "LUMIGO_COLD_START_KEY";
    private static final String TRIGGERED_BY_FALLBACK = "No recognized trigger";
    private static final int MAXIMAL_NUMBER_OF_MESSAGE_IDS = 50;

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
    public static TriggeredBy extractTriggeredByFromEvent(Object event) {
        TriggeredBy triggeredBy = new TriggeredBy();
        try {
            Logger.info(
                    "Trying to find triggered by to event from class {}",
                    event != null ? event.getClass().getName() : null);
            if (event == null) {
                return null;
            } else if (event instanceof DynamodbEvent) {
                triggeredBy.setTriggeredBy("dynamodb");
                if (((DynamodbEvent) event).getRecords() != null
                        && ((DynamodbEvent) event).getRecords().size() > 0) {
                    DynamodbEvent.DynamodbStreamRecord firstRecord =
                            ((DynamodbEvent) event).getRecords().get(0);
                    triggeredBy.setArn(firstRecord.getEventSourceARN());
                    if (firstRecord.getDynamodb() != null
                            && firstRecord.getDynamodb().getApproximateCreationDateTime() != null) {
                        triggeredBy.setApproxEventCreationTime(
                                firstRecord
                                        .getDynamodb()
                                        .getApproximateCreationDateTime()
                                        .getTime());
                    }
                    List<String> messageIds =
                            ((DynamodbEvent) event)
                                    .getRecords().stream()
                                            .map(AwsUtils::extractMessageIdFromDynamodbRecord)
                                            .filter(Objects::nonNull)
                                            .limit(MAXIMAL_NUMBER_OF_MESSAGE_IDS)
                                            .collect(Collectors.toList());
                    if (messageIds.size() > 0) triggeredBy.setMessageIds(messageIds);
                }
            } else if (event instanceof KinesisEvent) {
                triggeredBy.setTriggeredBy("kinesis");
                if (((KinesisEvent) event).getRecords() != null
                        && ((KinesisEvent) event).getRecords().size() > 0) {
                    List<KinesisEvent.KinesisEventRecord> records =
                            ((KinesisEvent) event).getRecords();
                    triggeredBy.setArn(records.get(0).getEventSourceARN());
                    List<String> messageIds =
                            records.stream()
                                    .map(
                                            kinesisEventRecord ->
                                                    kinesisEventRecord
                                                            .getKinesis()
                                                            .getSequenceNumber())
                                    .collect(Collectors.toList());
                    triggeredBy.setMessageId(records.get(0).getKinesis().getSequenceNumber());
                    triggeredBy.setMessageIds(messageIds);
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
            } else if (instanceofByName(event, "S3Event")) {
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
                    triggeredBy.setMessageId(
                            ((SNSEvent) event).getRecords().get(0).getSNS().getMessageId());
                }
            } else if (event instanceof SQSEvent) {
                triggeredBy.setTriggeredBy("sqs");
                if (((SQSEvent) event).getRecords() != null
                        && ((SQSEvent) event).getRecords().size() > 0) {
                    triggeredBy.setArn(((SQSEvent) event).getRecords().get(0).getEventSourceArn());
                    triggeredBy.setMessageId(((SQSEvent) event).getRecords().get(0).getMessageId());
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
            } else if (event instanceof KafkaEvent) {
                triggeredBy.setTriggeredBy("kafka");
                triggeredBy.setArn(((KafkaEvent) event).getEventSourceArn());
                String topic = null;
                List<String> messageIds = new ArrayList<>();
                if (((KafkaEvent) event).getRecords() != null) {
                    for (Map.Entry<String, List<KafkaEvent.KafkaEventRecord>> entry :
                            ((KafkaEvent) event).getRecords().entrySet()) {
                        for (KafkaEvent.KafkaEventRecord record : entry.getValue()) {
                            if (topic == null) {
                                topic = record.getTopic();
                            }
                            for (Map<String, byte[]> headers : record.getHeaders()) {
                                if (headers.containsKey(KafkaSpan.LUMIGO_MESSAGE_ID_KEY)) {
                                    messageIds.add(
                                            new String(
                                                    headers.get(KafkaSpan.LUMIGO_MESSAGE_ID_KEY),
                                                    StandardCharsets.UTF_8));
                                    break;
                                }
                            }
                        }
                    }
                }
                triggeredBy.setResource(topic);
                triggeredBy.setMessageIds(
                        messageIds.subList(
                                0,
                                Math.min(
                                        messageIds.size(),
                                        Configuration.getInstance().maxBatchMessageIds())));
            } else {
                Logger.info(
                        "Failed to found relevant triggered by found for event {} ",
                        event.getClass().getName());
                triggeredBy.setTriggeredBy(TRIGGERED_BY_FALLBACK);
                return triggeredBy;
            }

            Logger.info("Found triggered by handler to event {}", event.getClass().getName());
            return triggeredBy;

        } catch (Throwable e) {
            Logger.error(e, "Failed to extract triggerBy data");
            triggeredBy.setTriggeredBy(TRIGGERED_BY_FALLBACK);
            return triggeredBy;
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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    @NoArgsConstructor
    @Data
    public static class TriggeredBy {
        private String triggeredBy;
        private String arn;
        private String httpMethod;
        private String resource;
        private String api;
        private String stage;
        private String messageId;
        private List<String> messageIds = Collections.emptyList();
        private long approxEventCreationTime = 0;

        public List<String> getMessageIds() {
            if (this.messageIds != null) return this.messageIds;
            return Collections.emptyList();
        }

        public void setMessageIds(List<String> messageIds) {
            this.messageIds = messageIds;
        }
    }

    public static synchronized Span.READINESS getFunctionReadiness() {
        if (System.getProperty(COLD_START_KEY) != null) {
            return Span.READINESS.WARM;
        } else {
            System.setProperty(COLD_START_KEY, "false");
            return Span.READINESS.COLD;
        }
    }

    public static int parseJavaVersion(String version) {
        try {
            String[] parts = version.split("\\.");

            if (parts[0].equals("1")) {
                // For version before Java 9 the version looks like: 1.X.minor.
                // example 1.8.0 or 1.5.0.
                return Integer.parseInt(parts[1]);
            }
            // From java 9 the version looks like: 9.0.1 or 11.2.1 or 21.0.11,
            // So we only parse the first part.
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            Logger.error(e, "Failed to parse java version");
            return -1;
        }
    }

    /**
     * This function seeks for the value of className in all the super classes of the given object.
     *
     * <p>We use this [weird] functionality because we prefer to not use directly the classes to
     * avoid big dependencies.
     */
    private static boolean instanceofByName(Object event, String className) {
        Class c = event.getClass();
        while (c != null) {
            if (c.getSimpleName().equals(className)) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static String extractMessageIdFromDynamodbRecord(
            DynamodbEvent.DynamodbStreamRecord record) {
        if (record.getEventName() == null) return null;
        if (record.getEventName().equals("INSERT")) {
            return calculateItemHash(record.getDynamodb().getNewImage());
        } else if (record.getEventName().equals("MODIFY")
                || record.getEventName().equals("REMOVE")) {
            return calculateItemHash(record.getDynamodb().getKeys());
        }
        return null;
    }

    private static String calculateItemHash(Map<String, AttributeValue> item) {
        Map<String, Object> simpleMap = convertAttributeMapToSimpleMap(item);
        return StringUtils.buildMd5Hash(JsonUtils.getObjectAsJsonString(simpleMap));
    }

    private static Map<String, Object> convertAttributeMapToSimpleMap(
            Map<String, AttributeValue> attributeValueMap) {
        Map<String, Object> simpleMap = new HashMap<>();
        attributeValueMap.forEach(
                (key, value) -> simpleMap.put(key, attributeValueToObject(value)));
        return simpleMap;
    }

    private static Object attributeValueToObject(AttributeValue value) {
        if (value == null) {
            return null;
        } else if (value.getS() != null) {
            return value.getS();
        } else if (value.getN() != null) {
            return value.getN();
        } else if (value.getBOOL() != null) {
            return value.getBOOL();
        } else if (value.getL() != null && !value.getL().isEmpty()) {
            List<Object> list = new ArrayList<>();
            for (AttributeValue v : value.getL()) {
                list.add(attributeValueToObject(v));
            }
            return list;
        } else if (value.getM() != null && !value.getM().isEmpty()) {
            return convertAttributeMapToSimpleMap(value.getM());
        }
        return null;
    }
}
