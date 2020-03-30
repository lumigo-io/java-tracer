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
    void getEnvIntSuccess() {
        assertEquals(2, envUtil.getEnvInt("FOR_LUMIGO_TEST", 1));
    }

    @Test
    void getEnvIntFail() {
        System.setProperty("TEST_KEY", "wrong");
        assertEquals(1, envUtil.getEnvInt("TEST_KEY", 1));
    }
}
