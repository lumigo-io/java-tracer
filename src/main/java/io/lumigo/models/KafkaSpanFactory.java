package io.lumigo.models;

import static io.lumigo.core.SpansContainer.KAFKA_SPAN_TYPE;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.internals.ProducerMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

@UtilityClass
public class KafkaSpanFactory {
    public static <K, V> KafkaSpan createProduce(
            Span baseSpan,
            Long startTime,
            Serializer<K> keySerializer,
            Serializer<V> valueSerializer,
            ProducerMetadata producerMetadata,
            ProducerRecord<K, V> record,
            RecordMetadata recordMetadata,
            Exception exception) {
        List<String> bootstrapServers =
                producerMetadata.fetch().nodes().stream()
                        .map(node -> node.host() + ":" + node.port())
                        .collect(Collectors.toList());
        String topic = record.topic();
        KafkaSpan.KafkaProducerRecord producerRecord =
                KafkaSpan.KafkaProducerRecord.builder()
                        .key(
                                keySerializer.serialize(
                                        record.topic(), record.headers(), record.key()))
                        .value(
                                valueSerializer.serialize(
                                        record.topic(), record.headers(), record.value()))
                        .headers(extractHeaders(record.headers()))
                        .build();

        KafkaSpan.KafkaProducerResponse response;
        if (exception == null) {
            response =
                    KafkaSpan.KafkaProducerSuccessResponse.builder()
                            .partition(recordMetadata.partition())
                            .offset(recordMetadata.offset())
                            .build();
        } else {
            response =
                    KafkaSpan.KafkaProducerErrorResponse.builder()
                            .errorMessage(exception.getMessage())
                            .build();
        }

        Header messageIdHeader = record.headers().lastHeader(KafkaSpan.LUMIGO_MESSAGE_ID_KEY);
        String messageId =
                messageIdHeader == null
                        ? null
                        : new String(messageIdHeader.value(), StandardCharsets.UTF_8);

        return new KafkaSpan.KafkaSpanBuilder()
                .id(UUID.randomUUID().toString())
                .started(startTime)
                .ended(System.currentTimeMillis())
                .type(KAFKA_SPAN_TYPE)
                .transactionId(baseSpan.getTransactionId())
                .account(baseSpan.getAccount())
                .region(baseSpan.getRegion())
                .token(baseSpan.getToken())
                .parentId(baseSpan.getId())
                .info(
                        KafkaSpan.Info.builder()
                                .tracer(
                                        KafkaSpan.Tracer.builder()
                                                .version(
                                                        baseSpan.getInfo().getTracer().getVersion())
                                                .build())
                                .traceId(
                                        KafkaSpan.TraceId.builder()
                                                .root(baseSpan.getInfo().getTraceId().getRoot())
                                                .build())
                                .messageId(messageId)
                                .kafkaInfo(
                                        KafkaSpan.KafkaProducerInfo.builder()
                                                .kafkaInfoType(KafkaSpan.KAFKA_PRODUCER_TYPE)
                                                .bootstrapServers(bootstrapServers)
                                                .topic(topic)
                                                .record(producerRecord)
                                                .response(response)
                                                .build())
                                .build())
                .build();
    }

    public static KafkaSpan createConsume(
            Span baseSpan,
            Long startTime,
            KafkaConsumer<?, ?> consumer,
            ConsumerMetadata consumerMetadata,
            ConsumerRecords<?, ?> consumerRecords) {
        List<String> messageIds = new ArrayList<>();
        List<String> bootstrapServers =
                consumerMetadata.fetch().nodes().stream()
                        .map(node -> node.host() + ":" + node.port())
                        .collect(Collectors.toList());
        List<String> topics = new ArrayList<>(consumer.subscription());
        List<KafkaSpan.KafkaConsumerRecord> records = new ArrayList<>();
        consumerRecords.forEach(
                record -> {
                    Header messageIdHeader =
                            record.headers().lastHeader(KafkaSpan.LUMIGO_MESSAGE_ID_KEY);
                    String messageId =
                            messageIdHeader == null
                                    ? null
                                    : new String(messageIdHeader.value(), StandardCharsets.UTF_8);
                    if (messageId != null) {
                        messageIds.add(messageId);
                    }
                    records.add(
                            KafkaSpan.KafkaConsumerRecord.builder()
                                    .topic(record.topic())
                                    .partition(record.partition())
                                    .offset(record.offset())
                                    .key(record.key().toString())
                                    .value(record.value().toString())
                                    .headers(extractHeaders(record.headers()))
                                    .build());
                });
        return KafkaSpan.builder()
                .id(UUID.randomUUID().toString())
                .started(startTime)
                .ended(System.currentTimeMillis())
                .type(KAFKA_SPAN_TYPE)
                .transactionId(baseSpan.getTransactionId())
                .account(baseSpan.getAccount())
                .region(baseSpan.getRegion())
                .token(baseSpan.getToken())
                .parentId(baseSpan.getId())
                .info(
                        KafkaSpan.Info.builder()
                                .tracer(
                                        KafkaSpan.Tracer.builder()
                                                .version(
                                                        baseSpan.getInfo().getTracer().getVersion())
                                                .build())
                                .traceId(
                                        KafkaSpan.TraceId.builder()
                                                .root(baseSpan.getInfo().getTraceId().getRoot())
                                                .build())
                                .messageIds(messageIds)
                                .kafkaInfo(
                                        KafkaSpan.KafkaConsumerInfo.builder()
                                                .kafkaInfoType(KafkaSpan.KAFKA_CONSUMER_TYPE)
                                                .bootstrapServers(bootstrapServers)
                                                .consumerGroupId(consumer.groupMetadata().groupId())
                                                .topics(topics)
                                                .recordsCount(consumerRecords.count())
                                                .records(records)
                                                .build())
                                .build())
                .build();
    }

    private static Map<String, byte[]> extractHeaders(Headers headers) {
        return Arrays.stream(headers.toArray())
                .collect(Collectors.toMap(Header::key, Header::value));
    }
}
