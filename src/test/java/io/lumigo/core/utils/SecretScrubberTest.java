package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class SecretScrubberTest {
    @Test
    @DisplayName("does not modify non-json payloads")
    void testSecretScrubbingUtils_does_not_scrub_non_json_payloads() {
        assertEquals("123", SecretScrubber.getInstance().scrubBody("123"));
    }

    @Test
    @DisplayName("scrubs a nested body value using LUMIGO_SECRET_MASKING_REGEX")
    void testSecretScrubbingUtils_scrubs_json_payload_default() {
        String actual =
                SecretScrubber.getInstance()
                        .scrubBody(
                                "{\"pass\":\"word\",\"key\":\"value\",\"secret\":\"stuff\",\"credential\":\"admin:admin\",\"passphrase\":\"SesameOpen\",\"SessionToken\":\"XyZ012x=\",\"x-amz-security-token\":\"amzToken123\",\"Signature\":\"yours truly\",\"authorization\":\"Bearer 123\"}");
        String expected =
                "{\"authorization\":\"****\",\"credential\":\"****\",\"pass\":\"****\",\"SessionToken\":\"****\",\"Signature\":\"****\",\"passphrase\":\"****\",\"secret\":\"****\",\"x-amz-security-token\":\"****\",\"key\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName(
            "scrubs body using the default patterns when LUMIGO_SECRET_MASKING_REGEX is invalid")
    @SetEnvironmentVariable(
            key = "LUMIGO_SECRET_MASKING_REGEX",
            value = "[THIS IS NOT A VALID JSON ARRAY]")
    void
            testSecretScrubbingUtils_scrubs_json_payload_default_used_when_invalid_pattern_list_provided() {
        String actual =
                SecretScrubber.getInstance()
                        .scrubBody(
                                "{\"pass\":\"word\",\"key\":\"value\",\"secret\":\"stuff\",\"credential\":\"admin:admin\",\"passphrase\":\"SesameOpen\",\"SessionToken\":\"XyZ012x=\",\"x-amz-security-token\":\"amzToken123\",\"Signature\":\"yours truly\",\"authorization\":\"Bearer 123\"}");
        String expected =
                "{\"authorization\":\"****\",\"credential\":\"****\",\"pass\":\"****\",\"SessionToken\":\"****\",\"Signature\":\"****\",\"passphrase\":\"****\",\"secret\":\"****\",\"x-amz-security-token\":\"****\",\"key\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("scrubs body using LUMIGO_SECRET_MASKING_REGEX - case insensitive")
    void testSecretScrubbingUtils_scrubs_json_payload_default_case_insensitive() {
        String actual =
                SecretScrubber.getInstance()
                        .scrubBody(
                                "{\"pAss\":\"word\",\"KeY\":\"value\",\"seCRet\":\"stuff\",\"CrEdEntial\":\"admin:admin\",\"paSSPhrase\":\"SesameOpen\",\"seSSIOntOkEn\":\"XyZ012x=\",\"X-AMZ-security-token\":\"amzToken123\",\"SIGnatUre\":\"yours truly\",\"AuTHOrization\":\"Bearer 123\"}");
        String expected =
                "{\"pAss\":\"****\",\"CrEdEntial\":\"****\",\"paSSPhrase\":\"****\",\"seSSIOntOkEn\":\"****\",\"SIGnatUre\":\"****\",\"seCRet\":\"****\",\"X-AMZ-security-token\":\"****\",\"KeY\":\"****\",\"AuTHOrization\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("scrubs body using LUMIGO_SECRET_MASKING_REGEX - top-level key")
    @SetEnvironmentVariable(key = "LUMIGO_SECRET_MASKING_REGEX", value = "[\".*topsecret.*\"]")
    void testSecretScrubbingUtils_scrubs_json_payload_top_level_key() {
        String actual = SecretScrubber.getInstance().scrubBody("{\"topsecret\":\"stuff\"}");
        String expected = "{\"topsecret\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("scrubs body using LUMIGO_SECRET_MASKING_REGEX - nested keys")
    @SetEnvironmentVariable(key = "LUMIGO_SECRET_MASKING_REGEX", value = "[\".*topsecret.*\"]")
    void testSecretScrubbingUtils_scrubs_json_payload_nested_key() {
        String actual =
                SecretScrubber.getInstance()
                        .scrubBody("{\"some\": {\"topsecret\":\"stuff\"}, \"a\": 1}");
        String expected = "{\"some\":{\"topsecret\":\"****\"},\"a\":1}";

        assertEquals(expected, actual);
    }
}
