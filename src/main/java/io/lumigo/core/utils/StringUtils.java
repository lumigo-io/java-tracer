package io.lumigo.core.utils;

import io.lumigo.core.configuration.Configuration;

public class StringUtils {

    private static final int MAX_STRING_SIZE = Configuration.getInstance().maxSpanFieldSize();

    public static String getMaxSizeString(String input) {
        if (input != null && input.length() > MAX_STRING_SIZE) {
            return input.substring(0, MAX_STRING_SIZE);
        }
        return input;
    }
}
