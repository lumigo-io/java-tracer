package io.lumigo.core.utils;

import io.lumigo.core.configuration.Configuration;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Random;
import org.pmw.tinylog.Logger;

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

    public static String extractStringForStream(InputStream inputStream, int size) {
        if (inputStream != null && inputStream.markSupported()) {
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                Logger.info("Stream reset supported, convert to string");
                byte[] buffer = new byte[size];
                int len = inputStream.read(buffer);
                byteArrayOutputStream.write(buffer, 0, len);
                String result =
                        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset());
                inputStream.reset();
                return result;
            } catch (Throwable e) {
                Logger.error(e, "Failed to extract string from stream");
                return null;
            } finally {

            }
        } else {
            Logger.info("Stream reset is null");
            return null;
        }
    }
}
