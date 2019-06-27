package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

    @Test
    void testGetObjectAsJsonString_null() throws Exception {
        assertNull(null, JsonUtils.getObjectAsJsonString(null));
    }

    @Test
    void testGetObjectAsJsonString_Object() throws Exception {
        assertEquals("{\"a\":1,\"b\":\"2\"}", JsonUtils.getObjectAsJsonString(new A()));
    }

    @Test
    void testGetObjectAsJsonString_string() throws Exception {
        assertEquals("aaa", JsonUtils.getObjectAsJsonString("aaa"));
    }

    @Test
    void testGetObjectAsJsonString_int() throws Exception {
        assertEquals("1", JsonUtils.getObjectAsJsonString(1));
    }

    @Test
    void testConvertStringToJson_valid() {
        JsonNode jsonNode = JsonUtils.convertStringToJson("{\"a\": 2}");
        assertEquals(2, jsonNode.get("a").asInt());
    }

    @Test
    void testConvertStringToJson_null() {
        assertNull(JsonUtils.convertStringToJson(null));
    }

    @Test
    void testConvertStringToJson_not_json() {
        assertNull(JsonUtils.convertStringToJson("{2}"));
    }

    public class A {
        private int a = 1;
        private String b = "2";

        public int getA() {
            return a;
        }

        public String getB() {
            return b;
        }
    }
}
