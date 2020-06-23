package io.lumigo.core.utils;

import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;
import org.pmw.tinylog.Logger;

public class StringUtils {

    private static final String candidateChars = "abcdefghijklmnopqrstuvwxyz1234567890";

    public static String getMaxSizeString(String input, int maxStringSize) {
        if (input != null && input.length() > maxStringSize) {
            return input.substring(0, maxStringSize);
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String buildMd5Hash(Map<String, AttributeValue> item) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String itemAsJson = ItemUtils.toItem(item).toJSON();
            md.update(itemAsJson.getBytes());
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e, "Failed to build hash of item");
            return null;
        }
    }
}
