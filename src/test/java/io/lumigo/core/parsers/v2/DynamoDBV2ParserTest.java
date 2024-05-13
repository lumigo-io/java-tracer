package io.lumigo.core.parsers.v2;

import io.lumigo.models.HttpSpan;

class DynamoDBV2ParserTest {

    private HttpSpan span = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
    private DynamoDBV2Parser dynamoDBParser = new DynamoDBV2Parser();
}
