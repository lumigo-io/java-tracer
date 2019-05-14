package com.lumigo.core.utils;

public class StringUtils {

  private static final int MAX_STRING_SIZE = 1024;

  public static String getMaxSizeString(String input) {
    if (input != null && input.length() > MAX_STRING_SIZE) {
      return input.substring(0, MAX_STRING_SIZE);
    }
    return input;
  }
}
