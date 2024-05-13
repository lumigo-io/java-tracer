package io.lumigo.core.utils;

import lombok.experimental.UtilityClass;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class AwsSdkV2Utils {

    public static Map<String, Object> convertAttributeMapToSimpleMap(Map<String, AttributeValue> attributeValueMap) {
        Map<String, Object> simpleMap = new HashMap<>();
        attributeValueMap.forEach((key, value) -> simpleMap.put(key, attributeValueToObject(value)));
        return simpleMap;
    }

    private static Object attributeValueToObject(AttributeValue value) {
        if (value.s() != null) {
            return value.s();
        } else if (value.n() != null) {
            return value.n();
        } else if (value.bool() != null) {
            return value.bool();
        } else if (value.m() != null) {
            return convertAttributeMapToSimpleMap(value.m());
        } else if (value.l() != null) {
            List<Object> list = new ArrayList<>();
            for (AttributeValue v : value.l()) {
                list.add(attributeValueToObject(v));
            }
            return list;
        }
        return null;
    }
}
