package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EnvUtilTest {

    EnvUtil envUtil = new EnvUtil();

    @Test
    void getEnv_by_property() {
        assertEquals(envUtil.getEnv("USER"), System.getenv("USER"));
    }

    @Test
    void getEnv() {
        for (String key : System.getenv().keySet()) {
            assertEquals(System.getenv(key), envUtil.getEnv(key));
        }
    }

    @Test
    void getEnv_string_array_default() {
        String[] deflt = {"a", "b"};

        assertEquals(deflt, envUtil.getStringArrayEnv("NOT_EXISTS", deflt));
    }

    @Test
    void getEnv_string_array() {
        String[] arr = {System.getenv("HOME")};

        String[] actual = envUtil.getStringArrayEnv("HOME", null);

        assertEquals(1, actual.length);
        assertEquals(arr[0], actual[0]);
    }
}
