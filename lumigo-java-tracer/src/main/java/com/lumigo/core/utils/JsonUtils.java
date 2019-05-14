package com.lumigo.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();


    /**
     *
     * @param o is any object
     * @return serialized json of any object as string, null if null, string if string
     */
    public static String getObjectAsJsonString(Object o) {
        try {
            if (o == null || o instanceof String) {
                return (String) o;
            }
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
