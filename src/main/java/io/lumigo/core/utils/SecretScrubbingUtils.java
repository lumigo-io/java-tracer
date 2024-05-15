package io.lumigo.core.utils;

import java.io.Console;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SecretScrubbingUtils {
  static String scrubBody(String body) {
    try {
      String secretMaskingRegex = System.getenv("LUMIGO_SECRET_MASKING_REGEX");

      if (secretMaskingRegex == null) {
        return body;
      }

      JSONArray rawRegexArray = new JSONArray(secretMaskingRegex);
      Pattern[] regexPatterns = IntStream.range(0, rawRegexArray.length())
          .mapToObj(i -> rawRegexArray.getString(i))
          .map(Pattern::compile)
          .toArray(Pattern[]::new);

      JSONObject jsonBody = new JSONObject(body);
      Iterator<String> jsonKeys = jsonBody.keys();

      while (jsonKeys.hasNext()) {
        String key = jsonKeys.next();
        String value = jsonBody.getString(key);

        boolean shouldBeMasked = Arrays.stream(regexPatterns).anyMatch(regex -> regex.matcher(value).find());
        System.out.println(key);

        if (shouldBeMasked) {
          jsonBody.put(key, "****");
        }
      }

      return jsonBody.toString();
    } catch (

    JSONException e) {
      return body;
    }
  }
}
