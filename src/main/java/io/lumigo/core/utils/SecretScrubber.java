package io.lumigo.core.utils;

import java.util.regex.Pattern;
import org.json.JSONObject;

public class SecretScrubber {
    private static SecretScrubbingPatternProvider secretScrubbingUtils =
            new SecretScrubbingPatternProvider();
    private static final String SECRET_PLACEHOLDER = "****";

    private SecretScrubber() {}

    String scrubBody(String body) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            return scrubJsonObject(jsonObject, secretScrubbingUtils.getBodyScrubbingPatterns())
                    .toString();
        } catch (Exception e) {
            return body;
        }
    }

    private JSONObject scrubJsonObject(JSONObject jsonObject, Pattern[] patterns) {
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

    private boolean isSecret(String value, Pattern[] patterns) {
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
