package com.lumigo.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param o Any object
     * @return object json representation as string
     * @throws JsonProcessingException the object can't be serialize to json
     */
    public static String getObjectAsJsonString(Object o) throws JsonProcessingException {
        if (o == null || o instanceof String) {
            return (String) o;
        }
        return mapper.writeValueAsString(o);
    }
}
