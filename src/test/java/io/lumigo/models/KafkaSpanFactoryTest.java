package io.lumigo.models;

import static org.mockito.Mockito.when;

import io.lumigo.core.utils.JsonUtils;
import java.util.Collections;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.internals.ProducerMetadata;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class KafkaSpanFactoryTest {
    private static final Long NOW = System.currentTimeMillis();
    private static final String BOOTSTRAP_SERVERS = "bootstrap-servers";
    private static final int PORT = 9092;
    private static final String TOPIC = "topic";
    private static final int PARTITION = 1;
    private static final long OFFSET = 12345L;
    private static final String GROUP_ID = "groupId";
    private Span baseSpan;
    private RecordMetadata recordMetadata;
    @Mock private ProducerMetadata producerMetadata;
    @Mock private KafkaConsumer<?, ?> consumer;
    @Mock private ConsumerMetadata consumerMetadata;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        baseSpan =
                Span.builder()
                        .started(1L)
                        .ended(2L)
                        .info(
                                Span.Info.builder()
                                        .tracer(Span.Tracer.builder().version("1.0").build())
                                        .traceId(Span.TraceId.builder().root("1-2-3").build())
                                        .approxEventCreationTime(12)
                                        .build())
                        .build();
        recordMetadata =
                new RecordMetadata(new TopicPartition(TOPIC, PARTITION), OFFSET, 0, 0, 0L, 0, 0);
        Cluster cluster =
                new Cluster(
                        "clusterId",
                        Collections.singletonList(new Node(0, BOOTSTRAP_SERVERS, PORT)),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        new Node(0, BOOTSTRAP_SERVERS, PORT));
        when(producerMetadata.fetch()).thenReturn(cluster);
        when(consumerMetadata.fetch()).thenReturn(cluster);
        when(consumer.subscription()).thenReturn(Collections.singleton(TOPIC));
        when(consumer.groupMetadata()).thenReturn(new ConsumerGroupMetadata(GROUP_ID));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateProduceSpan(boolean injectHeaders) throws Exception {
        ProducerRecord<String, String> producerRecord;
        String messageId;
        String headers;
        if (injectHeaders) {
            producerRecord =
                    new ProducerRecord<>(
                            "topic",
                            PARTITION,
                            "key",
                            "value",
                            Collections.singletonList(
                                    new RecordHeader("lumigoMessageId", "123".getBytes())));
            messageId = "\"123\"";
            headers = "'{\"lumigoMessageId\":\"MTIz\"}'";
        } else {
            producerRecord = new ProducerRecord<>("topic", PARTITION, "key", "value");
            messageId = null;
            headers = "'{}'";
        }

        KafkaSpan result =
                KafkaSpanFactory.createProduce(
                        baseSpan,
                        NOW,
                        new StringSerializer(),
                        new StringSerializer(),
                        producerMetadata,
                        producerRecord,
                        recordMetadata,
                        null);

        String expectedSpan =
                "{"
                        + "\"started\":1716208357697,"
                        + "\"ended\":1716208358649,"
                        + "\"id\":\"9746e565-9f1a-4b9c-92f7-9a63337e1193\","
                        + "\"type\":\"kafka\","
                        + "\"transactionId\":null,"
                        + "\"account\":null,"
                        + "\"region\":null,"
                        + "\"token\":null,"
                        + "\"parentId\":null,"
                        + "\"info\":"
                        + "   {"
                        + "       \"tracer\": {\"version\":\"1.0\"},"
                        + "       \"traceId\":{\"Root\":\"1-2-3\"},"
                        + "       \"kafkaInfo\":"
                        + "           {"
                        + "               \"bootstrapServers\": '[\"bootstrap-servers:9092\"]',"
                        + "               \"topic\":\"topic\","
                        + "               \"record\": {\"key\":\"key\",\"value\":\"value\",\"headers\":"
                        + headers
                        + "},"
                        + "               \"response\":{\"partition\":1,\"offset\":12345}"
                        + "           },"
                        + "       \"messageId\":"
                        + messageId
                        + ","
                        + "       \"messageIds\":null,"
                        + "       \"resourceName\":null,"
                        + "       \"targetArn\":null"
                        + "}"
                        + "}";
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(result),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("id", (o1, o2) -> o1 != null),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testCreateProduceSpanWithError(boolean injectHeaders) throws Exception {
        ProducerRecord<String, String> producerRecord;
        String messageId;
        String headers;
        if (injectHeaders) {
            producerRecord =
                    new ProducerRecord<>(
                            "topic",
                            PARTITION,
                            "key",
                            "value",
                            Collections.singletonList(
                                    new RecordHeader("lumigoMessageId", "123".getBytes())));
            messageId = "\"123\"";
            headers = "'{\"lumigoMessageId\":\"MTIz\"}'";
        } else {
            producerRecord = new ProducerRecord<>("topic", PARTITION, "key", "value");
            messageId = null;
            headers = "'{}'";
        }

        KafkaSpan result =
                KafkaSpanFactory.createProduce(
                        baseSpan,
                        NOW,
                        new StringSerializer(),
                        new StringSerializer(),
                        producerMetadata,
                        producerRecord,
                        recordMetadata,
                        new Exception("Failed to produce message"));

        String expectedSpan =
                "{"
                        + "\"started\":1716208357697,"
                        + "\"ended\":1716208358649,"
                        + "\"id\":\"9746e565-9f1a-4b9c-92f7-9a63337e1193\","
                        + "\"type\":\"kafka\","
                        + "\"transactionId\":null,"
                        + "\"account\":null,"
                        + "\"region\":null,"
                        + "\"token\":null,"
                        + "\"parentId\":null,"
                        + "\"info\":"
                        + "   {"
                        + "       \"tracer\": {\"version\":\"1.0\"},"
                        + "       \"traceId\":{\"Root\":\"1-2-3\"},"
                        + "       \"kafkaInfo\":"
                        + "           {"
                        + "               \"bootstrapServers\": '[\"bootstrap-servers:9092\"]',"
                        + "               \"topic\":\"topic\","
                        + "               \"record\": {\"key\":\"key\",\"value\":\"value\",\"headers\":"
                        + headers
                        + "},"
                        + "               \"response\":{\"errorMessage\": \"Failed to produce message\"}"
                        + "           },"
                        + "       \"messageId\":"
                        + messageId
                        + ","
                        + "       \"messageIds\":null,"
                        + "       \"resourceName\":null,"
                        + "       \"targetArn\":null"
                        + "}"
                        + "}";
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(result),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("id", (o1, o2) -> o1 != null),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null)));
    }

    @Test
    public void testCreateConsumeSpan() throws Exception {
        ConsumerRecords<?, ?> consumerRecords =
                new ConsumerRecords<>(
                        Collections.singletonMap(
                                new TopicPartition(TOPIC, PARTITION),
                                Collections.singletonList(
                                        new ConsumerRecord<>(
                                                TOPIC, PARTITION, 0, "key", "value"))));
        KafkaSpan result =
                KafkaSpanFactory.createConsume(
                        baseSpan, NOW, consumer, consumerMetadata, consumerRecords);

        String expectedSpan =
                "{"
                        + "\"started\":1716210606909,"
                        + "\"ended\":1716210608628,"
                        + "\"id\":\"19abee7e-67a7-4263-882c-3f251163913b\","
                        + "\"type\":\"kafka\","
                        + "\"transactionId\":null,"
                        + "\"account\":null,"
                        + "\"region\":null,"
                        + "\"token\":null,"
                        + "\"parentId\":null,"
                        + "\"info\":{"
                        + "   \"tracer\":{\"version\":\"1.0\"},"
                        + "   \"traceId\":{\"Root\":\"1-2-3\"},"
                        + "   \"kafkaInfo\":"
                        + "       {"
                        + "       \"bootstrapServers\":[\"bootstrap-servers:9092\"],"
                        + "       \"consumerGroupId\":\"groupId\","
                        + "       \"recordsCount\":1,"
                        + "       \"topics\":[\"topic\"],"
                        + "       \"records\":[{\"topic\":\"topic\",\"partition\":1,\"offset\":0,\"key\":\"key\",\"value\":\"value\",\"headers\":'{}'}]"
                        + "       },"
                        + "   \"messageId\":null,"
                        + "   \"messageIds\":[],"
                        + "   \"resourceName\":null,"
                        + "   \"targetArn\":null"
                        + "}"
                        + "}";
        JSONAssert.assertEquals(
                expectedSpan,
                JsonUtils.getObjectAsJsonString(result),
                new CustomComparator(
                        JSONCompareMode.LENIENT,
                        new Customization("info.tracer.version", (o1, o2) -> o1 != null),
                        new Customization("id", (o1, o2) -> o1 != null),
                        new Customization("started", (o1, o2) -> o1 != null),
                        new Customization("ended", (o1, o2) -> o1 != null)));
    }
}
