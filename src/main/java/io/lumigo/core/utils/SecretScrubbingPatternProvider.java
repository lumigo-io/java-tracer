package io.lumigo.core.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SecretScrubbingPatternProvider {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final List<String> DEFAULT_PATTERN_STRINGS =
            Arrays.asList(
                    ".*pass.*",
                    ".*key.*",
                    ".*secret.*",
                    ".*credential.*",
                    ".*passphrase.*",
                    "SessionToken",
                    "x-amz-security-token",
                    "Signature",
                    "Authorization");
    private static final List<Pattern> DEFAULT_PATTERNS =
            stringListToPatterns(DEFAULT_PATTERN_STRINGS);

    private static List<Pattern> stringListToPatterns(List<String> patternStrings) {
        ArrayList<Pattern> patterns = new ArrayList<>();
        for (String patternString : patternStrings) {
            patterns.add(Pattern.compile(patternString, Pattern.CASE_INSENSITIVE));
        }

        return patterns;
    }

    private List<Pattern> jsonListToPatternList(String jsonList) throws IOException {
        List<String> patternStrings = new ArrayList<String>();

        try (JsonParser parser = JSON_FACTORY.createParser(jsonList)) {
            if (!JsonToken.START_ARRAY.equals(parser.nextToken())) {
                throw new IllegalArgumentException();
            }
            while (!JsonToken.END_ARRAY.equals(parser.nextToken())) {
                if (parser.currentToken().equals(JsonToken.VALUE_STRING)) {
                    patternStrings.add(parser.getText());
                } else {
                    throw new IllegalArgumentException();
                }
            }

            return stringListToPatterns(patternStrings);
        }
    }

    public List<Pattern> getBodyScrubbingPatterns(Map<String, String> env) {
        String regexStringifiedList = env.get("LUMIGO_SECRET_MASKING_REGEX");

        if (Strings.isBlank(regexStringifiedList)) {
            return DEFAULT_PATTERNS;
        }

        try {
            return jsonListToPatternList(regexStringifiedList);
        } catch (IOException e) {
            return DEFAULT_PATTERNS;
        }
    }
}
