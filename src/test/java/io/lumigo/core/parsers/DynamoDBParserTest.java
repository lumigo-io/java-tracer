package io.lumigo.core.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.dynamodbv2.model.*;
import com.sun.tools.javac.util.List;
import io.lumigo.models.HttpSpan;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DynamoDBParserTest {

    private HttpSpan span = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
    private DynamoDBParser dynamoDBParser = new DynamoDBParser();
    @Mock Request request;
    @Mock GetItemRequest getItemRequest;
    @Mock BatchGetItemRequest batchGetItemRequest;
    @Mock PutItemRequest putItemRequest;
    @Mock UpdateItemRequest updateItemRequest;
    @Mock DeleteItemRequest deleteItemRequest;
    @Mock BatchWriteItemRequest batchWriteItemRequest;
    Response response;
    Map<String, AttributeValue> item;
    String itemHash;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        item = Collections.singletonMap("k", new AttributeValue("v"));
        itemHash = "44244ce1a15ee6d4dc270001564cb759";
    }

    @Test
    void test_parse_ddb_unknown_request() {
        when(request.getOriginalRequest()).thenReturn(AmazonWebServiceRequest.NOOP);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_get_item() {
        when(getItemRequest.getTableName()).thenReturn("tableName");
        when(request.getOriginalRequest()).thenReturn(getItemRequest);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(HttpSpan.Info.builder().resourceName("tableName").build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_batch_get_item() {
        when(batchGetItemRequest.getRequestItems())
                .thenReturn(Collections.singletonMap("tableName", new KeysAndAttributes()));
        when(request.getOriginalRequest()).thenReturn(batchGetItemRequest);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(HttpSpan.Info.builder().resourceName("tableName").build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_put_item() {
        when(putItemRequest.getTableName()).thenReturn("tableName");
        when(putItemRequest.getItem()).thenReturn(item);
        when(request.getOriginalRequest()).thenReturn(putItemRequest);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName("tableName")
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_update_item() {
        when(updateItemRequest.getTableName()).thenReturn("tableName");
        when(updateItemRequest.getKey()).thenReturn(item);
        when(request.getOriginalRequest()).thenReturn(updateItemRequest);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName("tableName")
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_delete_item() {
        when(deleteItemRequest.getTableName()).thenReturn("tableName");
        when(deleteItemRequest.getKey()).thenReturn(item);
        when(request.getOriginalRequest()).thenReturn(deleteItemRequest);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName("tableName")
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_batch_write_item() {
        when(batchWriteItemRequest.getRequestItems())
                .thenReturn(
                        Collections.singletonMap(
                                "tableName", List.of(new WriteRequest(new PutRequest(item)))));
        when(request.getOriginalRequest()).thenReturn(batchWriteItemRequest);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName("tableName")
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }

    @Test
    void test_parse_ddb_batch_delete_item() {
        when(batchWriteItemRequest.getRequestItems())
                .thenReturn(
                        Collections.singletonMap(
                                "tableName", List.of(new WriteRequest(new DeleteRequest(item)))));
        when(request.getOriginalRequest()).thenReturn(batchWriteItemRequest);

        dynamoDBParser.safeParse(span, request, response);

        HttpSpan expectedSpan =
                HttpSpan.builder()
                        .info(
                                HttpSpan.Info.builder()
                                        .resourceName("tableName")
                                        .messageId(itemHash)
                                        .build())
                        .build();
        assertEquals(span, expectedSpan);
    }
}
