package io.lumigo.core.utils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class SecretScrubber {
    private static final SecretScrubbingPatternProvider secretScrubbingUtils =
            new SecretScrubbingPatternProvider();
    private static final String SECRET_PLACEHOLDER = "****";

    private SecretScrubber() {}

    public String scrubBody(String body, Map<String, String> env) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            return scrubJsonObject(jsonObject, secretScrubbingUtils.getBodyScrubbingPatterns(env))
                    .toString();
        } catch (Exception e) {
            return body;
        }
    }

    private JSONObject scrubJsonObject(JSONObject jsonObject, List<Pattern> patterns) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);

            if (value instanceof String && isSecret(key, patterns)) {
                jsonObject.put(key, SECRET_PLACEHOLDER);
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

    public static SecretScrubber getInstance() {
        return new SecretScrubber();
    }
}
