package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.lumigo.core.configuration.Configuration;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecretScrubberTest {
    static final Map<String, String> noScrubbingOverridesEnv = new HashMap<String, String>();

    @Test
    @DisplayName("does not modify non-json payloads")
    void testSecretScrubbingUtils_does_not_scrub_non_json_payloads() {
        assertEquals("123", new SecretScrubber(noScrubbingOverridesEnv).scrubBody("123"));
    }

    @Test
    @DisplayName("scrubs a nested body value with default expressions")
    void testSecretScrubbingUtils_scrubs_json_payload_default() {
        String actual =
                new SecretScrubber(noScrubbingOverridesEnv)
                        .scrubBody(
                                "{\"pass\":\"word\",\"key\":\"value\",\"secret\":\"stuff\",\"credential\":\"admin:admin\",\"passphrase\":\"SesameOpen\",\"SessionToken\":\"XyZ012x=\",\"x-amz-security-token\":\"amzToken123\",\"Signature\":\"yours truly\",\"authorization\":\"Bearer 123\"}");
        String expected =
                "{\"authorization\":\"****\",\"credential\":\"****\",\"pass\":\"****\",\"SessionToken\":\"****\",\"Signature\":\"****\",\"passphrase\":\"****\",\"secret\":\"****\",\"x-amz-security-token\":\"****\",\"key\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName(
            "scrubs body using the default patterns when LUMIGO_SECRET_MASKING_REGEX is invalid")
    void
            testSecretScrubbingUtils_scrubs_json_payload_default_used_when_invalid_pattern_list_provided() {
        String actual =
                new SecretScrubber(envWithMaskingRegex("[THIS IS NOT A VALID JSON ARRAY]"))
                        .scrubBody(
                                "{\"pass\":\"word\",\"key\":\"value\",\"secret\":\"stuff\",\"credential\":\"admin:admin\",\"passphrase\":\"SesameOpen\",\"SessionToken\":\"XyZ012x=\",\"x-amz-security-token\":\"amzToken123\",\"Signature\":\"yours truly\",\"authorization\":\"Bearer 123\"}");
        String expected =
                "{\"authorization\":\"****\",\"credential\":\"****\",\"pass\":\"****\",\"SessionToken\":\"****\",\"Signature\":\"****\",\"passphrase\":\"****\",\"secret\":\"****\",\"x-amz-security-token\":\"****\",\"key\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("scrubs body using default expressions - case insensitive")
    void testSecretScrubbingUtils_scrubs_json_payload_default_case_insensitive() {
        String actual =
                new SecretScrubber(noScrubbingOverridesEnv)
                        .scrubBody(
                                "{\"pAss\":\"word\",\"KeY\":\"value\",\"seCRet\":\"stuff\",\"CrEdEntial\":\"admin:admin\",\"paSSPhrase\":\"SesameOpen\",\"seSSIOntOkEn\":\"XyZ012x=\",\"X-AMZ-security-token\":\"amzToken123\",\"SIGnatUre\":\"yours truly\",\"AuTHOrization\":\"Bearer 123\"}");
        String expected =
                "{\"pAss\":\"****\",\"CrEdEntial\":\"****\",\"paSSPhrase\":\"****\",\"seSSIOntOkEn\":\"****\",\"SIGnatUre\":\"****\",\"seCRet\":\"****\",\"X-AMZ-security-token\":\"****\",\"KeY\":\"****\",\"AuTHOrization\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("scrubs body using LUMIGO_SECRET_MASKING_REGEX - top-level key")
    void testSecretScrubbingUtils_scrubs_json_payload_top_level_key() {
        String actual =
                new SecretScrubber(envWithMaskingRegex("[\".*topsecret.*\"]"))
                        .scrubBody("{\"topsecret\":\"stuff\"}");
        String expected = "{\"topsecret\":\"****\"}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("scrubs body using LUMIGO_SECRET_MASKING_REGEX - nested keys")
    void testSecretScrubbingUtils_scrubs_json_payload_nested_key() {
        String actual =
                new SecretScrubber(envWithMaskingRegex("[\".*topsecret.*\"]"))
                        .scrubBody("{\"some\": {\"topsecret\":\"stuff\"}, \"a\": 1}");

        String expected = "{\"some\":{\"topsecret\":\"****\"},\"a\":1}";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("scrubs headers using default expressions")
    void testSecretScrubbingUtils_scrubs_headers_using_default_expressions() {
        Header[] actual =
                new SecretScrubber(noScrubbingOverridesEnv)
                        .scrubHeaders(
                                new Header[] {
                                    new BasicHeader("passphrase", "secret"),
                                    new BasicHeader("thisisnot", "value")
                                });
        ;

        assertEquals(actual[0].getName(), "passphrase");
        assertEquals(actual[0].getValue(), "****");

        assertEquals(actual[1].getName(), "thisisnot");
        assertEquals(actual[1].getValue(), "value");
    }

    @Test
    @DisplayName("scrubs headers using LUMIGO_SECRET_MASKING_REGEX")
    void testSecretScrubbingUtils_scrubs_headers_using_env_var() {
        Header[] actual =
                new SecretScrubber(envWithMaskingRegex(".*thisissecret.*"))
                        .scrubHeaders(
                                new Header[] {
                                    new BasicHeader("thisissecretyo", "secret"),
                                    new BasicHeader("thisisnot", "value")
                                });
        ;

        assertEquals(actual[0].getName(), "thisissecretyo");
        assertEquals(actual[0].getValue(), "****");

        assertEquals(actual[1].getName(), "thisisnot");
        assertEquals(actual[1].getValue(), "value");
    }

    @Test
    @DisplayName("scrubs environment variables using default expressions")
    void testSecretScrubbingUtils_scrubs_env_vars_using_default_expressions() {
        Map<String, String> originalEnv =
                new HashMap<String, String>() {
                    {
                        put("PASSPHRASE", "secret");
                        put("ENV", "dev");
                    }
                };

        new SecretScrubber(envWithMaskingRegex(".*thisissecret.*")).scrubEnv(originalEnv);

        Map<String, String> scrubbedEnv = new SecretScrubber(originalEnv).scrubEnv(originalEnv);

        // Make sure the original env stays unaffected
        assertEquals(originalEnv.get("PASSPHRASE"), "secret");
        assertEquals(originalEnv.get("ENV"), "dev");

        assertEquals(scrubbedEnv.get("PASSPHRASE"), "****");
        assertEquals(scrubbedEnv.get("ENV"), "dev");
    }

    @Test
    @DisplayName("scrubs environment variables using LUMIGO_SECRET_MASKING_REGEX")
    void testSecretScrubbingUtils_scrubs_env_vars_using_env_var() {
        Map<String, String> originalEnv =
                new HashMap<String, String>() {
                    {
                        put("thisissecretyo", "secret");
                        put("thisisnot", "value");
                    }
                };

        new SecretScrubber(envWithMaskingRegex(".*thisissecret.*")).scrubEnv(originalEnv);

        Map<String, String> scrubbedEnv = new SecretScrubber(originalEnv).scrubEnv(originalEnv);

        // Make sure the original env stays unaffected
        assertEquals(originalEnv.get("thisissecretyo"), "secret");
        assertEquals(originalEnv.get("thisisnot"), "value");

        assertEquals(scrubbedEnv.get("thisissecretyo"), "****");
        assertEquals(scrubbedEnv.get("thisisnot"), "value");
    }

    private Map<String, String> envWithMaskingRegex(String maskingRegex) {
        return new HashMap<String, String>() {
            {
                put(Configuration.LUMIGO_SECRET_MASKING_REGEX, maskingRegex);
            }
        };
    }
}
