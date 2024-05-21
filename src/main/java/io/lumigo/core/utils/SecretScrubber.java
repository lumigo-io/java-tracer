package io.lumigo.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class SecretScrubber {
    private SecretScrubbingPatternProvider secretScrubbingUtils;

    private static final String SECRET_PLACEHOLDER = "****";

    public SecretScrubber(Map<String, String> env) {
        this.secretScrubbingUtils = new SecretScrubbingPatternProvider(env);
    }

    public String scrubStringifiedObject(String stringifiedObject) {
        try {
            JSONObject jsonObject = new JSONObject(stringifiedObject);
            return scrubJsonObject(jsonObject, secretScrubbingUtils.getBodyScrubbingPatterns())
                    .toString();
        } catch (Exception e) {
            return stringifiedObject;
        }
    }

    private JSONObject scrubJsonObject(JSONObject jsonObject, List<Pattern> patterns) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            if (value instanceof String && isSecret(key, patterns)) {
                jsonObject.put(key, SECRET_PLACEHOLDER);
            } else if (value instanceof JSONArray) {
                ArrayList<Object> scrubbedArray = new ArrayList<>();

                for (Object item : (JSONArray) value) {
                    if (item instanceof String && isSecret(key, patterns)) {
                        scrubbedArray.add(SECRET_PLACEHOLDER);
                    } else if (item instanceof JSONObject) {
                        scrubbedArray.add(scrubJsonObject((JSONObject) item, patterns));
                    } else {
                        scrubbedArray.add(item);
                    }
                }

                jsonObject.put(key, scrubbedArray.toArray());

            } else if (value instanceof JSONObject) {
                jsonObject.put(key, scrubJsonObject((JSONObject) value, patterns));
            }
        }

        return jsonObject;
    }

    private boolean isSecret(String value, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).matches()) {
                return true;
            }
        }

        return false;
    }
}
