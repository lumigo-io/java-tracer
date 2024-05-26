package io.lumigo.models;

import io.lumigo.core.utils.SecretScrubber;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class KafkaSpanTest {

    @Test
    public void testScrub() throws Exception {
        KafkaSpan kafkaSpan =
                KafkaSpan.builder()
                        .info(
                                KafkaSpan.Info.builder()
                                        .kafkaInfo(
                                                KafkaSpan.KafkaProducerInfo.builder()
                                                        .bootstrapServers(
                                                                StringUtils.repeat(
                                                                        "bootstrapServer,", 10))
                                                        .record(
                                                                KafkaSpan.KafkaProducerRecord
                                                                        .builder()
                                                                        .key("{\"key\":\"value\"}")
                                                                        .value(
                                                                                StringUtils.repeat(
                                                                                        "value,",
                                                                                        10))
                                                                        .headers(
                                                                                StringUtils.repeat(
                                                                                        "headers,",
                                                                                        10))
                                                                        .build())
                                                        .build())
                                        .build())
                        .build();

        KafkaSpan result =
                (KafkaSpan)
                        kafkaSpan.scrub(
                                new SecretScrubber(
                                        Collections.singletonMap(
                                                "LUMIGO_SECRET_MASKING_REGEX", ".*key.*")));
        ;
        Assert.assertEquals(
                "{\"key\":\"****\"}",
                ((KafkaSpan.KafkaProducerInfo) result.getInfo().getKafkaInfo())
                        .getRecord()
                        .getKey());
    }

    @Test
    public void testReduceSizeProduce() throws Exception {

        KafkaSpan kafkaSpan =
                KafkaSpan.builder()
                        .info(
                                KafkaSpan.Info.builder()
                                        .kafkaInfo(
                                                KafkaSpan.KafkaProducerInfo.builder()
                                                        .bootstrapServers(
                                                                StringUtils.repeat(
                                                                        "bootstrapServer,", 10))
                                                        .record(
                                                                KafkaSpan.KafkaProducerRecord
                                                                        .builder()
                                                                        .key(
                                                                                StringUtils.repeat(
                                                                                        "key,", 10))
                                                                        .value(
                                                                                StringUtils.repeat(
                                                                                        "value,",
                                                                                        10))
                                                                        .headers(
                                                                                StringUtils.repeat(
                                                                                        "headers,",
                                                                                        10))
                                                                        .build())
                                                        .build())
                                        .build())
                        .build();

        KafkaSpan result = (KafkaSpan) kafkaSpan.reduceSize(10);

        assert ((KafkaSpan.KafkaProducerInfo) result.getInfo().getKafkaInfo())
                        .getBootstrapServers()
                        .length()
                == 10;
        assert ((KafkaSpan.KafkaProducerInfo) result.getInfo().getKafkaInfo())
                        .getRecord()
                        .getKey()
                        .length()
                == 10;
        assert ((KafkaSpan.KafkaProducerInfo) result.getInfo().getKafkaInfo())
                        .getRecord()
                        .getValue()
                        .length()
                == 10;
        assert ((KafkaSpan.KafkaProducerInfo) result.getInfo().getKafkaInfo())
                        .getRecord()
                        .getHeaders()
                        .length()
                == 10;
    }
}
