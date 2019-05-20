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
}
