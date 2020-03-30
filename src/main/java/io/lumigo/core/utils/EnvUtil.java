package io.lumigo.core.utils;

import java.util.Map;

public class EnvUtil {
    public String getEnv(String key) {
        return System.getenv(key);
    }

    public Integer getEnvInt(String key, Integer dflt) {
        try {
            return Integer.valueOf(getEnv(key));
        } catch (Exception e) {
            return dflt;
        }
    }

    public Boolean getBooleanEnv(String key, Boolean dflt) {
        String value = getEnv(key);
        return value == null ? dflt : "true".equalsIgnoreCase(value);
    }

    public Map<String, String> getEnv() {
        return System.getenv();
    }
}
