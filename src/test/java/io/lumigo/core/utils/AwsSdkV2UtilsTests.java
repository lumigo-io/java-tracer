package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class AwsSdkV2UtilsTests {

    private Map<String, AttributeValue> attributeValueMap;

    @BeforeEach
    void setUp() {
        attributeValueMap = new HashMap<>();
        attributeValueMap.put("key", AttributeValue.builder().s("value").build());
        attributeValueMap.put("key2", AttributeValue.builder().n("2").build());
        attributeValueMap.put("key3", AttributeValue.builder().bool(true).build());
        attributeValueMap.put(
                "key4",
                AttributeValue.builder()
                        .m(
                                Collections.singletonMap(
                                        "key", AttributeValue.builder().s("value4").build()))
                        .build());
        attributeValueMap.put(
                "key5",
                AttributeValue.builder()
                        .l(
                                AttributeValue.builder().s("value5").build(),
                                AttributeValue.builder().s("value5.2").build())
                        .build());
    }

    @Test
    void testCalculateItemHash() {
        String result = AwsSdkV2Utils.calculateItemHash(attributeValueMap);

        assertEquals("10ac224af47748812c94ade5be937d59", result);
    }

    @Test
    void testConvertAttributeMapToSimpleMap() {
        Map<String, Object> result =
                AwsSdkV2Utils.convertAttributeMapToSimpleMap(attributeValueMap);

        assertEquals(attributeValueMap.size(), result.size());
        assertEquals("value", result.get("key"));
        assertEquals("2", result.get("key2"));
        assertEquals(true, result.get("key3"));
        assertEquals(Collections.singletonMap("key", "value4"), result.get("key4"));
        assertArrayEquals(
                Lists.newArrayList("value5", "value5.2").toArray(),
                ((ArrayList) result.get("key5")).toArray());
    }
}
