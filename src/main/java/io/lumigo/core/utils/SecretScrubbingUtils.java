package io.lumigo.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class SecretScrubbingUtils {
  protected static final JsonFactory JSON_FACTORY = new JsonFactory();

  static String scrubBody(String body) {
    try {
      JSONObject jsonObject = new JSONObject(body);
      String regExps = System.getenv("LUMIGO_SECRET_MASKING_REGEX");
      final List<Pattern> patternList;

      if (Strings.isBlank(regExps)) {
        return body;
      }

      patternList = new ArrayList<>();
      try (JsonParser parser = JSON_FACTORY.createParser(regExps)) {
        if (!JsonToken.START_ARRAY.equals(parser.nextToken())) {
          throw new IllegalArgumentException();
        }
        while (!JsonToken.END_ARRAY.equals(parser.nextToken())) {
          if (parser.currentToken().equals(JsonToken.VALUE_STRING)) {
            patternList.add(Pattern.compile(parser.getText(), Pattern.CASE_INSENSITIVE));
          } else {
            throw new IllegalArgumentException();
          }
        }

        Pattern[] patterns = patternList.toArray(new Pattern[0]);
        return scrubJsonObject(jsonObject, patterns).toString();
      } catch (Exception e1) {
        return body;
      }
    } catch (Exception e2) {
      return body;
    }
  }

  static JSONObject scrubJsonObject(JSONObject jsonObject, Pattern[] patterns) {
    for (String key : jsonObject.keySet()) {
      Object value = jsonObject.get(key);

      if (value instanceof String && SecretScrubbingUtils.isSecret(key, patterns)) {
        jsonObject.put(key, "****");
      } else if (value instanceof JSONObject) {
        jsonObject.put(key, scrubJsonObject((JSONObject) value, patterns));
      }
    }

    return jsonObject;
  }

  private static boolean isSecret(String value, Pattern[] patterns) {
    for (Pattern pattern : patterns) {
      if (pattern.matcher(value).matches()) {
        return true;
      }
    }

    return false;
  }
}
