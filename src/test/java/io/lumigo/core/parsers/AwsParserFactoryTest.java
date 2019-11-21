package io.lumigo.core.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AwsParserFactoryTest {

    @Test
    public void test_check_non_supported_value() {
        assertEquals(DefaultParser.class, AwsParserFactory.getParser("Not supported").getClass());
    }

    @Test
    public void test_check_null_value() {
        assertEquals(DefaultParser.class, AwsParserFactory.getParser(null).getClass());
    }

    @Test
    public void test_check_sns_value() {
        assertEquals(SnsParser.class, AwsParserFactory.getParser("AmazonSNS").getClass());
    }

    @Test
    public void test_check_sqs_value() {
        assertEquals(SqsParser.class, AwsParserFactory.getParser("AmazonSQS").getClass());
    }
}
