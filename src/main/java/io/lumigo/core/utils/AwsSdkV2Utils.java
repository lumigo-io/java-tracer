package io.lumigo.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@UtilityClass
public class AwsSdkV2Utils {

    public String calculateItemHash(
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item) {
        Map<String, Object> simpleMap = AwsSdkV2Utils.convertAttributeMapToSimpleMap(item);
        return StringUtils.buildMd5Hash(JsonUtils.getObjectAsJsonString(simpleMap));
    }

    public static Map<String, Object> convertAttributeMapToSimpleMap(
            Map<String, AttributeValue> attributeValueMap) {
        Map<String, Object> simpleMap = new HashMap<>();
        attributeValueMap.forEach(
                (key, value) -> simpleMap.put(key, attributeValueToObject(value)));
        return simpleMap;
    }

    private static Object attributeValueToObject(AttributeValue value) {
        if (value == null) {
            return null;
        } else if (value.s() != null) {
            return value.s();
        } else if (value.n() != null) {
            return value.n();
        } else if (value.bool() != null) {
            return value.bool();
        } else if (value.l() != null && !value.l().isEmpty()) {
            List<Object> list = new ArrayList<>();
            for (AttributeValue v : value.l()) {
                list.add(attributeValueToObject(v));
            }
            return list;
        } else if (value.m() != null && !value.m().isEmpty()) {
            return convertAttributeMapToSimpleMap(value.m());
        }
        return null;
    }
}
