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
                int read = inputStream.read(buffer);
                if (read > 0) {
                    Logger.debug("Read {} bytes from input stream", read);
                    byteArrayOutputStream.write(buffer, 0, size);
                    String result =
                            new String(
                                            byteArrayOutputStream.toByteArray(),
                                            Charset.defaultCharset())
                                    .trim();
                    inputStream.reset();
                    return result;
                } else {
                    Logger.info("No bytes can be read from stream");
                }

            } catch (Throwable e) {
                Logger.error(e, "Failed to extract string from stream");

            }
        } else {
            Logger.info("Stream markSupported is false or stream is null");
        }
        return null;
    }
}
