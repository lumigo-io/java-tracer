package com.lumigo.core.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwsUtils {

    /**
     * @param arn an arn of the with the format arn:aws:lambda:<region>:<account>:function:<name>
     * @return The account id, or null if we failed to parse
     */
    public static String extractAwsAccountFromArn(String arn) {
        String[] arnParts = arn.split(":");
        if (arnParts.length < 7) {
            return null;
        }
        return arnParts[4];
    }

    /**
     * TODO - not in the MVP.
     *
     * @param event
     * @return
     */
    public static Map<String, String> extractTriggeredByFromEvent(Object event) {
        return null;
    }

    /**
     * @param amznTraceId with the form `Root=1-2-3;Another=456;Bla=789`
     * @return The value of the root phrase
     */
    public static String extractAwsTraceRoot(String amznTraceId) {
        Matcher matcher = Pattern.compile("([^;]+)=([^;]*)").matcher(amznTraceId);
        while (matcher.find()) {
            if (matcher.group(1).equals("Root")) {
                return matcher.group(2);
            }
        }
        return null;
    }

    /**
     * @param amznTraceId with the form `Root=1-2-3;Another=456;Bla=789`
     * @return The the third part in the root phrase
     */
    public static String extractAwsTraceTransactionId(String amznTraceId) {
        String root = extractAwsTraceRoot(amznTraceId);
        if (root == null) {
            return null;
        }
        String[] rootParts = root.split("-");
        if (rootParts.length < 3) {
            return null;
        }
        return rootParts[2];
    }

    public static String extractAwsTraceSuffix(String amznTraceId) {
        if (!amznTraceId.contains(";")) {
            return amznTraceId;
        }
        return amznTraceId.substring(amznTraceId.indexOf(";"));
    }

}
