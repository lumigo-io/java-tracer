package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class SecretScrubbingUtilsTest {
  @Test
  @DisplayName("does not modify non-json payload")
  void testSecretScrubbingUtils_does_not_scrub_non_json_payloads() {
    assertEquals("123", SecretScrubbingUtils.scrubBody("123"));
  }

  @Test
  @DisplayName("scrubs the body using LUMIGO_SECRET_MASKING_REGEX")
  @SetEnvironmentVariable(key = "LUMIGO_SECRET_MASKING_REGEX", value = "[\".*topsecret.*\"]")
  void testSecretScrubbingUtils_scrubs_json_payload_top_level_key() {
    String actual = SecretScrubbingUtils.scrubBody("{\"topsecret\":\"stuff\"}");
    String expected = "{\"topsecret\":\"****\"}";

    assertEquals(expected, actual);
  }

  @Test
  @DisplayName("scrubs a nested body value using LUMIGO_SECRET_MASKING_REGEX")
  @SetEnvironmentVariable(key = "LUMIGO_SECRET_MASKING_REGEX", value = "[\".*topsecret.*\"]")
  void testSecretScrubbingUtils_scrubs_json_payload_nested_key() {
    String actual = SecretScrubbingUtils.scrubBody("{\"some\": {\"topsecret\":\"stuff\"}, \"a\": 1}");
    String expected = "{\"some\":{\"topsecret\":\"****\"},\"a\":1}";

    assertEquals(expected, actual);
  }
}
