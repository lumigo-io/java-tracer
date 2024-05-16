package io.lumigo.core.parsers.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AwsSdkV1ParserFactoryTest {

    @Test
    public void test_check_non_supported_value() {
        assertEquals(
                DoNothingV1Parser.class,
                AwsSdkV1ParserFactory.getParser("Not supported").getClass());
    }

    @Test
    public void test_check_null_value() {
        assertEquals(DoNothingV1Parser.class, AwsSdkV1ParserFactory.getParser(null).getClass());
    }

    @Test
    public void test_check_sns_value() {
        assertEquals(SnsV1Parser.class, AwsSdkV1ParserFactory.getParser("AmazonSNS").getClass());
    }

    @Test
    public void test_check_sqs_value() {
        assertEquals(SqsV1Parser.class, AwsSdkV1ParserFactory.getParser("AmazonSQS").getClass());
    }

    @Test
    public void test_check_kinesis_value() {
        assertEquals(
                KinesisV1Parser.class, AwsSdkV1ParserFactory.getParser("AmazonKinesis").getClass());
    }

    @Test
    public void test_check_dynamodb_value() {
        assertEquals(
                DynamoDBV1Parser.class,
                AwsSdkV1ParserFactory.getParser("AmazonDynamoDB").getClass());
    }
}
