package com.lumigo.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AwsUtilsTest {

  @Test
  @DisplayName("The case where the arn is not parsable")
  void testExtractAwsAccountFromArn_unparasbleArn() {
    assertNull(AwsUtils.extractAwsAccountFromArn("123"));
  }

  @Test
  void testExtractAwsAccountFromArn_happyFlow() {
    String exampleArn =
        "arn:aws:lambda:us-west-2:723663554526:function:tracer-test-saart-main-java8";
    assertEquals("723663554526", AwsUtils.extractAwsAccountFromArn(exampleArn));
  }

  @Test
  void testExtractAwsTraceRoot_happyFlow() {
    assertEquals("1-2-3", AwsUtils.extractAwsTraceRoot("Root=1-2-3;Another=456;Bla=789"));
  }

  @Test
  void testExtractAwsTraceRoot_noRoot() {
    assertNull(AwsUtils.extractAwsTraceRoot("Another=456;Bla=789"));
  }

  @Test
  void testExtractAwsTraceRoot_invalidFormat() {
    assertNull(AwsUtils.extractAwsTraceRoot("{'Root': 1234}"));
  }

  @Test
  void testExtractAwsTraceTransactionId_happyFlow() {
    assertEquals("3", AwsUtils.extractAwsTraceTransactionId("Root=1-2-3;Another=456;Bla=789"));
  }

  @Test
  void testExtractAwsTraceTransactionId_smallRoot() {
    assertNull(AwsUtils.extractAwsTraceTransactionId("Root=1-2;Another=456;Bla=789"));
  }

  @Test
  void testExtractAwsTraceTransactionId_noRoot() {
    assertNull(AwsUtils.extractAwsTraceTransactionId("Another=456;Bla=789"));
  }

  @Test
  void testExtractAwsTraceSuffix_happyFlow() {
    assertEquals(
        ";Another=456;Bla=789", AwsUtils.extractAwsTraceSuffix("Root=1-2-3;Another=456;Bla=789"));
  }

  @Test
  void testExtractAwsTraceSuffix_noSemicolon() {
    assertEquals("Another=456", AwsUtils.extractAwsTraceSuffix("Another=456"));
  }
}
