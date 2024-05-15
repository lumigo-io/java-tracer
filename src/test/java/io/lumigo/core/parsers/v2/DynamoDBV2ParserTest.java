package io.lumigo.core.parsers.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.tools.javac.util.List;
import io.lumigo.core.utils.AwsSdkV2Utils;
import io.lumigo.models.HttpSpan;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.services.dynamodb.model.*;

class DynamoDBV2ParserTest {

    public static final String TABLE_NAME = "tableName";

    private final DynamoDBV2Parser dynamoDBParser = new DynamoDBV2Parser();

    private HttpSpan span;

    private Map<String, AttributeValue> item;

    private String itemHash;

    @BeforeEach
    void setUp() {
        span = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
        item = Collections.singletonMap("k", AttributeValue.builder().s("v").build());
        itemHash = AwsSdkV2Utils.calculateItemHash(item);
    }

    @Test
    void test_parse_ddb_get_item() {
        GetItemRequest request = GetItemRequest.builder().tableName(TABLE_NAME).build();
        GetItemResponse response = GetItemResponse.builder().build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(request).response(response).build();

        dynamoDBParser.safeParse(span, context);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(HttpSpan.Info.builder().resourceName(TABLE_NAME).build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_batch_get_item() {
        BatchGetItemRequest request =
                BatchGetItemRequest.builder()
                        .requestItems(
                                Collections.singletonMap(
                                        TABLE_NAME, KeysAndAttributes.builder().build()))
                        .build();
        BatchGetItemResponse response = BatchGetItemResponse.builder().build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(request).response(response).build();

        dynamoDBParser.safeParse(span, context);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(HttpSpan.Info.builder().resourceName(TABLE_NAME).build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_put_item() {
        PutItemRequest request = PutItemRequest.builder().tableName(TABLE_NAME).item(item).build();
        BatchGetItemResponse response = BatchGetItemResponse.builder().build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(request).response(response).build();

        dynamoDBParser.safeParse(span, context);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName(TABLE_NAME)
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_update_item() {
        UpdateItemRequest request =
                UpdateItemRequest.builder().tableName(TABLE_NAME).key(item).build();
        UpdateItemResponse response = UpdateItemResponse.builder().build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(request).response(response).build();

        dynamoDBParser.safeParse(span, context);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName(TABLE_NAME)
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_delete_item() {
        DeleteItemRequest request =
                DeleteItemRequest.builder().tableName(TABLE_NAME).key(item).build();
        DeleteItemResponse response = DeleteItemResponse.builder().build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(request).response(response).build();

        dynamoDBParser.safeParse(span, context);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName(TABLE_NAME)
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_batch_write_item() {
        BatchWriteItemRequest request =
                BatchWriteItemRequest.builder()
                        .requestItems(
                                Collections.singletonMap(
                                        TABLE_NAME,
                                        List.of(
                                                WriteRequest.builder()
                                                        .putRequest(
                                                                PutRequest.builder()
                                                                        .item(item)
                                                                        .build())
                                                        .build())))
                        .build();
        BatchWriteItemResponse response = BatchWriteItemResponse.builder().build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(request).response(response).build();

        dynamoDBParser.safeParse(span, context);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName(TABLE_NAME)
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_batch_delete_item() {
        BatchWriteItemRequest request =
                BatchWriteItemRequest.builder()
                        .requestItems(
                                Collections.singletonMap(
                                        TABLE_NAME,
                                        List.of(
                                                WriteRequest.builder()
                                                        .deleteRequest(
                                                                DeleteRequest.builder()
                                                                        .key(item)
                                                                        .build())
                                                        .build())))
                        .build();
        BatchWriteItemResponse response = BatchWriteItemResponse.builder().build();
        Context.AfterExecution context =
                InterceptorContext.builder().request(request).response(response).build();

        dynamoDBParser.parse(span, context);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName(TABLE_NAME)
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }
}
