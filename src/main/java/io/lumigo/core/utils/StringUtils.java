package io.lumigo.core.utils;

import io.lumigo.core.configuration.Configuration;
import java.util.Random;

public class StringUtils {

    private static final int MAX_STRING_SIZE = Configuration.getInstance().maxSpanFieldSize();
    private static final String candidateChars = "abcdefghijklmnopqrstuvwxyz1234567890";

    public static String getMaxSizeString(String input) {
        if (input != null && input.length() > MAX_STRING_SIZE) {
            return input.substring(0, MAX_STRING_SIZE);
        }
        return input;
    }

    public static String randomStringAndNumbers(int size) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }

        return sb.toString();
    }
}
