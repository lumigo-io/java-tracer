package io.lumigo.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.pmw.tinylog.Logger;

public class ExecutionTags {
    private static final int MAX_TAG_KEY_LEN = 100;
    private static final int MAX_TAG_VALUE_LEN = 100;
    private static final int MAX_TAGS = 50;
    private static final String ADD_TAG_ERROR_MSG_PREFIX = "Error adding tag";

    private static final List<Map<String, String>> tags = new ArrayList<>();

    private ExecutionTags() {}

    private static class Holder {
        private static final ExecutionTags INSTANCE = new ExecutionTags();
    }

    // Method to return the singleton instance
    private static ExecutionTags getInstance() {
        return Holder.INSTANCE;
    }

    private static boolean validateTag(String key, String value, boolean shouldLogErrors) {
        key = String.valueOf(key);
        value = String.valueOf(value);
        if (key.isEmpty() || key.length() > MAX_TAG_KEY_LEN) {
            if (shouldLogErrors) {
                Logger.error(String.format("%s: key length should be between 1 and %d: %s - %s",
                        ADD_TAG_ERROR_MSG_PREFIX, MAX_TAG_KEY_LEN, key, value));
            }
            return false;
        }
        if (value.isEmpty() || value.length() > MAX_TAG_VALUE_LEN) {
            if (shouldLogErrors) {
                Logger.error(String.format("%s: value length should be between 1 and %d: %s - %s",
                        ADD_TAG_ERROR_MSG_PREFIX, MAX_TAG_VALUE_LEN, key, value));
            }
            return false;
        }
        if (tags.size() >= MAX_TAGS) {
            if (shouldLogErrors) {
                Logger.error(String.format("%s: maximum number of tags is %d: %s - %s",
                        ADD_TAG_ERROR_MSG_PREFIX, MAX_TAGS, key, value));
            }
            return false;
        }
        return true;
    }

    private static String normalizeTag(Object val) {
        return (val == null) ? null : String.valueOf(val);
    }

    public static void addTag(String key, String value, boolean shouldLogErrors) {
        try {
            Logger.info(String.format("Adding tag: %s - %s", key, value));
            if (!validateTag(key, value, shouldLogErrors)) {
                return;
            }
            Map<String, String> tag = new HashMap<>();
            tag.put("key", normalizeTag(key));
            tag.put("value", normalizeTag(value));
            tags.add(tag);
        } catch (Exception err) {
            if (shouldLogErrors) {
                Logger.error(ADD_TAG_ERROR_MSG_PREFIX);
            }
            Logger.error(err.getMessage());
            Logger.error(String.format("%s - %s", ADD_TAG_ERROR_MSG_PREFIX, err.getMessage()));
        }
    }

    public static List<Map<String, String>> getTags() {
        return new ArrayList<>(tags);
    }

    public static void clear() {
        tags.clear();
    }
}
