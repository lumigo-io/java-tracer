package io.lumigo.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pmw.tinylog.Logger;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param o Any object
     * @return object json representation as string
     */
    public static String getObjectAsJsonString(Object o) {
        if (o == null || o instanceof String) {
            return (String) o;
        }
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            Logger.error(e, "Failed converting to json class {}", o.getClass().getName());
            return null;
        }
    }

    /**
     * @param o Any object
     * @return object json
     */
    public static JsonNode convertStringToJson(String o) {
        if (o == null) {
            return null;
        }
        try {
            return mapper.readTree(o);
        } catch (Exception e) {
            Logger.error(e, "Failed converting to string to json {}", o);
            return null;
        }
    }
}
