package io.lumigo.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;

public class SecretScrubber {
    private SecretScrubbingPatternProvider secretScrubbingUtils;

    private static final String SECRET_PLACEHOLDER = "****";

    public SecretScrubber(Map<String, String> env) {
        this.secretScrubbingUtils = new SecretScrubbingPatternProvider(env);
    }

    public String scrubBody(String body) {
        try {
            JSONObject jsonObject = new JSONObject(body);
            return scrubJsonObject(jsonObject, secretScrubbingUtils.getBodyScrubbingPatterns())
                    .toString();
        } catch (Exception e) {
            return body;
        }
    }

    public Header[] scrubHeaders(Header[] headers) {
        ArrayList<Header> scrubbedHeaders = new ArrayList<>();

        for (Header header : headers) {
            if (isSecret(header.getName(), secretScrubbingUtils.getBodyScrubbingPatterns())) {
                scrubbedHeaders.add(new BasicHeader(header.getName(), SECRET_PLACEHOLDER));
            } else {
                scrubbedHeaders.add(header);
            }
        }

        return scrubbedHeaders.toArray(new Header[0]);
    }

    public Map<String, String> scrubEnv(Map<String, String> env) {
        Map<String, String> scrubbedEnv = new HashMap<>(env);

        for (String key : env.keySet()) {
            if (isSecret(key, secretScrubbingUtils.getBodyScrubbingPatterns())) {
                scrubbedEnv.put(key, SECRET_PLACEHOLDER);
            }
        }

        return scrubbedEnv;
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
}
