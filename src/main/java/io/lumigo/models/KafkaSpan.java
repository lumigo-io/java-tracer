package io.lumigo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lumigo.core.utils.SecretScrubber;
import java.util.*;

import io.lumigo.core.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class KafkaSpan implements BaseSpan {
    public static final String LUMIGO_MESSAGE_ID_KEY = "lumigoMessageId";
    public static final String KAFKA_PRODUCER_TYPE = "PRODUCER";
    public static final String KAFKA_CONSUMER_TYPE = "CONSUMER";

    private Long started;
    private Long ended;
    private String id;
    private String type;
    private String transactionId;
    private String account;
    private String region;
    private String token;
    private String parentId;
    private Info info;

    @Builder(toBuilder = true)
    @Data
    public static class Info {
        private KafkaSpan.Tracer tracer;
        private KafkaSpan.TraceId traceId;
        private KafkaSpan.KafkaInfo kafkaInfo;
        private String messageId;
        private List<String> messageIds;
        private String resourceName;
        private String targetArn;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class TraceId {
        @JsonProperty("Root")
        private String root;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class Tracer {
        private String version;
    }

    public interface KafkaInfo {}

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class KafkaProducerInfo implements KafkaInfo {
        private String kafkaInfoType;
        private String bootstrapServers;
        private String topic;
        private KafkaSpan.KafkaProducerRecord record;
        private KafkaSpan.KafkaProducerResponse response;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class KafkaProducerRecord {
        private String key;
        private String value;
        private String headers;
    }

    public interface KafkaProducerResponse {}

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class KafkaProducerSuccessResponse implements KafkaProducerResponse {
        private Integer partition;
        private Long offset;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class KafkaProducerErrorResponse implements KafkaProducerResponse {
        private String errorMessage;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class KafkaConsumerInfo implements KafkaInfo {
        private String kafkaInfoType;
        private List<String> bootstrapServers;
        private String consumerGroupId;
        private Integer recordsCount;
        private List<String> topics;
        private List<KafkaSpan.KafkaConsumerRecord> records;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    public static class KafkaConsumerRecord {
        private String topic;
        private Integer partition;
        private Long offset;
        private String key;
        private String value;
        private String headers;
    }

    @Override
    public BaseSpan scrub(SecretScrubber scrubber) {
        if (this.info.kafkaInfo instanceof KafkaProducerInfo) {
            KafkaProducerInfo kafkaProducerInfo = (KafkaProducerInfo) this.info.kafkaInfo;
            if (kafkaProducerInfo.getRecord() != null) {
                kafkaProducerInfo.getRecord().setKey(scrubber.scrubStringifiedObject(kafkaProducerInfo.getRecord().getKey()));
                kafkaProducerInfo.getRecord().setValue(scrubber.scrubStringifiedObject(kafkaProducerInfo.getRecord().getValue()));
                kafkaProducerInfo.getRecord().setHeaders(scrubber.scrubStringifiedObject(kafkaProducerInfo.getRecord().getHeaders()));
            }
            if (kafkaProducerInfo.getResponse() instanceof KafkaProducerErrorResponse) {
                KafkaProducerErrorResponse kafkaProducerErrorResponse = (KafkaProducerErrorResponse) kafkaProducerInfo.getResponse();
                kafkaProducerErrorResponse.setErrorMessage(scrubber.scrubStringifiedObject(kafkaProducerErrorResponse.getErrorMessage()));
            }
        } else if (this.info.kafkaInfo instanceof KafkaConsumerInfo) {
            KafkaConsumerInfo kafkaConsumerInfo = (KafkaConsumerInfo) this.info.kafkaInfo;
            if (kafkaConsumerInfo.getRecords() != null) {
                for (KafkaConsumerRecord record : kafkaConsumerInfo.getRecords()) {
                    record.setKey(scrubber.scrubStringifiedObject(record.getKey()));
                    record.setValue(scrubber.scrubStringifiedObject(record.getValue()));
                    record.setHeaders(scrubber.scrubStringifiedObject(record.getHeaders()));
                }
            }
        }
        return this;
    }

    @Override
    public BaseSpan reduceSize(int maxFieldSize) {
        if (this.info.kafkaInfo instanceof KafkaProducerInfo) {
            KafkaProducerInfo kafkaProducerInfo = (KafkaProducerInfo) this.info.kafkaInfo;
            if (kafkaProducerInfo.getRecord() != null) {
                kafkaProducerInfo.getRecord().setKey(StringUtils.getMaxSizeString(kafkaProducerInfo.getRecord().getKey(), maxFieldSize));
                kafkaProducerInfo.getRecord().setValue(StringUtils.getMaxSizeString(kafkaProducerInfo.getRecord().getValue(), maxFieldSize));
                kafkaProducerInfo.getRecord().setHeaders(StringUtils.getMaxSizeString(kafkaProducerInfo.getRecord().getHeaders(), maxFieldSize));
            }
            if (kafkaProducerInfo.getResponse() instanceof KafkaProducerErrorResponse) {
                KafkaProducerErrorResponse kafkaProducerErrorResponse = (KafkaProducerErrorResponse) kafkaProducerInfo.getResponse();
                kafkaProducerErrorResponse.setErrorMessage(StringUtils.getMaxSizeString(kafkaProducerErrorResponse.getErrorMessage(), maxFieldSize));
            }
        } else if (this.info.kafkaInfo instanceof KafkaConsumerInfo) {
            KafkaConsumerInfo kafkaConsumerInfo = (KafkaConsumerInfo) this.info.kafkaInfo;
            if (kafkaConsumerInfo.getRecords() != null) {
                for (KafkaConsumerRecord record : kafkaConsumerInfo.getRecords()) {
                    record.setKey(StringUtils.getMaxSizeString(record.getKey(), maxFieldSize));
                    record.setValue(StringUtils.getMaxSizeString(record.getValue(), maxFieldSize));
                    record.setHeaders(StringUtils.getMaxSizeString(record.getHeaders(), maxFieldSize));
                }
            }
        }
        return this;
    }
}
