package com.lumigo.core.utils;

import java.util.Map;

public class EnvUtil {
    public String getEnv(String key) {
        return System.getenv(key);
    }

    public Map<String, String> getEnv() {
        return System.getenv();
    }
}
