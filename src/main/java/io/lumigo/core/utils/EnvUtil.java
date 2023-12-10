package io.lumigo.core.utils;

import java.util.Map;

public class EnvUtil {
    public String getEnv(String key) {
        return System.getenv(key);
    }

    public Boolean getBooleanEnv(String key, Boolean dflt) {
        String value = getEnv(key);
        return value == null ? dflt : "true".equalsIgnoreCase(value);
    }

    public Integer getIntegerEnv(String key, Integer dflt) {
        try {
            String value = getEnv(key);
            return Integer.valueOf(value);
        } catch (Exception ignored) {
        }
        return dflt;
    }

    public String[] getStringArrayEnv(String key, String[] dflt) {
        String value = getEnv(key);
        if (value != null && !value.isEmpty()) {
            return value.split(",");
        }
        return dflt;
    }

    public Map<String, String> getEnv() {
        return System.getenv();
    }
}
