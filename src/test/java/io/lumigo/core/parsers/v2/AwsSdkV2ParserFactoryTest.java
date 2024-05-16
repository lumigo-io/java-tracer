package io.lumigo.core.parsers.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AwsSdkV2ParserFactoryTest {

    @Test
    public void test_check_non_supported_value() {
        assertEquals(
                DoNothingV2Parser.class,
                AwsSdkV2ParserFactory.getParser("Not supported").getClass());
    }

    @Test
    public void test_check_null_value() {
        assertEquals(DoNothingV2Parser.class, AwsSdkV2ParserFactory.getParser(null).getClass());
    }

    @Test
    public void test_check_sns_value_v2() {
        assertEquals(SnsV2Parser.class, AwsSdkV2ParserFactory.getParser("Sns").getClass());
    }

    @Test
    public void test_check_sqs_value_v2() {
        assertEquals(SqsV2Parser.class, AwsSdkV2ParserFactory.getParser("Sqs").getClass());
    }

    @Test
    public void test_check_kinesis_value_v2() {
        assertEquals(KinesisV2Parser.class, AwsSdkV2ParserFactory.getParser("Kinesis").getClass());
    }

    @Test
    public void test_check_dynamodb_value_v2() {
        assertEquals(
                DynamoDBV2Parser.class, AwsSdkV2ParserFactory.getParser("DynamoDb").getClass());
    }
}
