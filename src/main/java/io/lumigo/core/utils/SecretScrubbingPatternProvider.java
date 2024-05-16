package io.lumigo.core.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SecretScrubbingPatternProvider {
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final String[] DEFAULT_PATTERN_STRINGS = {
        ".*pass.*",
        ".*key.*",
        ".*secret.*",
        ".*credential.*",
        ".*passphrase.*",
        "SessionToken",
        "x-amz-security-token",
        "Signature",
        "Authorization"
    };
    private static final Pattern[] DEFAULT_PATTERNS = stringListToPatterns(DEFAULT_PATTERN_STRINGS);

    private static Pattern[] stringListToPatterns(String[] patternStrings) {
        ArrayList<Pattern> patterns = new ArrayList<>();
        for (String patternString : patternStrings) {
            patterns.add(Pattern.compile(patternString, Pattern.CASE_INSENSITIVE));
        }

        return patterns.toArray(new Pattern[0]);
    }

    private Pattern[] jsonListToPatternList(String jsonList) throws IOException {
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

            return stringListToPatterns(patternStrings.toArray(new String[0]));
        }
    }

    public Pattern[] getBodyScrubbingPatterns() {
        String regexStringifiedList = System.getenv("LUMIGO_SECRET_MASKING_REGEX");

        if (Strings.isBlank(regexStringifiedList)) {
            return DEFAULT_PATTERNS;
        } else {
            try {
                return jsonListToPatternList(regexStringifiedList);
            } catch (IOException e) {
                return DEFAULT_PATTERNS;
            }
        }
    }
}
