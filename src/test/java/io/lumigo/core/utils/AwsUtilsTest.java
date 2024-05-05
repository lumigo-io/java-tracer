package io.lumigo.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import infa.AwsLambdaEventGenerator;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

class AwsUtilsTest {

    AwsLambdaEventGenerator awsLambdaEventGenerator = new AwsLambdaEventGenerator();

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
                ";Another=456;Bla=789",
                AwsUtils.extractAwsTraceSuffix("Root=1-2-3;Another=456;Bla=789"));
    }

    @Test
    void testExtractAwsTraceSuffix_noSemicolon() {
        assertEquals("Another=456", AwsUtils.extractAwsTraceSuffix("Another=456"));
    }

    @Test
    void test_extractTriggeredByFromEvent_null() {
        assertNull(AwsUtils.extractTriggeredByFromEvent(null));
    }

    @Test
    void test_extractTriggeredByFromEvent_s3() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"s3\", \"arn\": \"s3-arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(awsLambdaEventGenerator.s3Event())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_dynamodb_with_partial_event() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"dynamodb\", \"arn\": \"dynamodb-arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.dynamodbPartialEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_dynamodb_with_full_event() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"dynamodb\", \"arn\": \"dynamodb-arn\", \"messageIds\": [\"44244ce1a15ee6d4dc270001564cb759\", \"0ba280a753e516f855d0b62a52d0b390\"], \"approxEventCreationTime\": 769554000}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.dynamodbEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_dynamodb_with_unknown_event() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"dynamodb\", \"arn\": \"dynamodb-arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.dynamodbUnknownEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_kinesisEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"kinesis\", \"arn\": \"kinesis-arn\", \"messageId\":\"1\", messageIds: [\"1\"]}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.kinesisEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_kinesisFirehoseEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"kinesis\", \"arn\": \"kinesis-arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.kinesisFirehoseEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_kinesisAnalyticsFirehoseInputPreprocessingEvent()
            throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"kinesis\", \"arn\": \"kinesis-arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator
                                        .kinesisAnalyticsFirehoseInputPreprocessingEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_kinesisAnalyticsStreamsInputPreprocessingEvent()
            throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"kinesis\", \"arn\": \"kinesis-arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator
                                        .kinesisAnalyticsStreamsInputPreprocessingEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_sqsEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"sqs\", \"arn\": \"sqs-arn\", \"messageId\": \"sqs-message-id\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(awsLambdaEventGenerator.sqsEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_snsEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"sns\", \"arn\": \"sns-arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(awsLambdaEventGenerator.snsEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_apigw() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"apigw\","
                        + " \"api\": \"www.host.com\","
                        + " \"httpMethod\": \"method\","
                        + " \"stage\": \"stage\","
                        + " \"resource\": \"resource\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.apiGatewayProxyRequestEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_cloudWatchLogsEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"cloudwatch\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.cloudWatchLogsEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_cloudFrontEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"cloudfront\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.cloudFrontEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_cognitoEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"cognito\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.cognitoEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_codeCommitEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"codecommit\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.codeCommitEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_lexEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"lex\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(awsLambdaEventGenerator.lexEvent())),
                true);
    }

    @Test
    void test_extractTriggeredByFromEvent_scheduledEvent() throws JSONException {
        JSONAssert.assertEquals(
                "{\"triggeredBy\": \"cloudwatch\", \"arn\": \"arn\"}",
                JsonUtils.getObjectAsJsonString(
                        AwsUtils.extractTriggeredByFromEvent(
                                awsLambdaEventGenerator.scheduledEvent())),
                true);
    }

    @ParameterizedTest
    @CsvSource({
        "1.5.0, 5",
        "1.8.151_a, 8",
        "10.0.1, 10",
        "17.0.10, 17",
        "21.0.3, 21",
        "22.0.1, 22",
        "notRealVersion, -1"
    })
    void test_parseJavaVersion_oldVersion(String rawVersion, int expectedVersion) throws Exception {
        int version = AwsUtils.parseJavaVersion(rawVersion);

        assertEquals(expectedVersion, version);
    }
}
