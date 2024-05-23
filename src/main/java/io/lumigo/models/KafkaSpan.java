package io.lumigo.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lumigo.core.utils.SecretScrubber;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
    @Getter
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
    @Getter
    public static class TraceId {
        @JsonProperty("Root")
        private String root;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Getter
    public static class Tracer {
        private String version;
    }

    public interface KafkaInfo {}

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Getter
    public static class KafkaProducerInfo implements KafkaInfo {
        private String kafkaInfoType;
        private List<String> bootstrapServers;
        private String topic;
        private KafkaSpan.KafkaProducerRecord record;
        private KafkaSpan.KafkaProducerResponse response;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Getter
    public static class KafkaProducerRecord {
        private byte[] key;
        private byte[] value;
        private Map<String, byte[]> headers;
    }

    public interface KafkaProducerResponse {}

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Getter
    public static class KafkaProducerSuccessResponse implements KafkaProducerResponse {
        private Integer partition;
        private Long offset;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Getter
    public static class KafkaProducerErrorResponse implements KafkaProducerResponse {
        private String errorMessage;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Getter
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
    @Getter
    public static class KafkaConsumerRecord {
        private String topic;
        private Integer partition;
        private Long offset;
        private String key;
        private String value;
        private Map<String, byte[]> headers;
    }

    @Override
    public BaseSpan scrub(SecretScrubber scrubber) {
        return null;
    }

    @Override
    public BaseSpan reduceSize(int maxFieldSize) {
        return null;
    }
}
