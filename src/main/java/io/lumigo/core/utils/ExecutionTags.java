package io.lumigo.core.utils;

import io.lumigo.models.Span.ExecutionTag;
import java.util.ArrayList;
import java.util.List;
import org.pmw.tinylog.Logger;

public class ExecutionTags {
    private static final int MAX_TAG_KEY_LEN = 100;
    private static final int MAX_TAG_VALUE_LEN = 100;
    private static final int MAX_TAGS = 50;
    private static final String ADD_TAG_ERROR_MSG_PREFIX = "Error adding tag";
    private final List<ExecutionTag> tags = new ArrayList<ExecutionTag>();

    private static final ExecutionTags ourInstance = new ExecutionTags();

    private ExecutionTags() {}

    public static ExecutionTags getInstance() {
        return ourInstance;
    }

    private boolean validateTag(String key, String value) {
        key = String.valueOf(key);
        value = String.valueOf(value);
        if (key.isEmpty() || key.length() > MAX_TAG_KEY_LEN) {
            Logger.debug(
                    String.format(
                            "%s: key length should be between 1 and %d: %s - %s",
                            ADD_TAG_ERROR_MSG_PREFIX, MAX_TAG_KEY_LEN, key, value));
            return false;
        }
        if (value.isEmpty() || value.length() > MAX_TAG_VALUE_LEN) {
            Logger.debug(
                    String.format(
                            "%s: value length should be between 1 and %d: %s - %s",
                            ADD_TAG_ERROR_MSG_PREFIX, MAX_TAG_VALUE_LEN, key, value));
            return false;
        }
        if (tags.size() >= MAX_TAGS) {
            Logger.debug(
                    String.format(
                            "%s: maximum number of tags is %d: %s - %s",
                            ADD_TAG_ERROR_MSG_PREFIX, MAX_TAGS, key, value));
            return false;
        }
        return true;
    }

    private String normalizeTag(Object val) {
        return (val == null) ? null : String.valueOf(val);
    }

    public void addTag(String key, String value, boolean shouldLogErrors) {
        try {
            Logger.debug(String.format("Adding tag: %s - %s", key, value));
            if (!validateTag(key, value)) {
                Logger.debug(String.format("Invalid tag not added: %s - %s", key, value));
                return;
            }
            tags.add(
                    ExecutionTag.builder()
                            .key(normalizeTag(key))
                            .value(normalizeTag(value))
                            .build());
        } catch (Exception err) {
            Logger.error(String.format("%s - %s", ADD_TAG_ERROR_MSG_PREFIX, err.getMessage()));
        }
    }

    public List<ExecutionTag> getTags() {
        return new ArrayList<>(tags);
    }

    public void clear() {
        this.tags.clear();
    }
}
