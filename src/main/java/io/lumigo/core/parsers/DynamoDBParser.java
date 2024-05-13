package io.lumigo.core.parsers;

import static io.lumigo.core.utils.StringUtils.dynamodbItemToHash;
import static io.lumigo.core.utils.StringUtils.dynamodbItemToHashV2;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.dynamodbv2.model.*;
import io.lumigo.models.HttpSpan;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

public class DynamoDBParser implements AwsParser {
    @Override
    public void parse(HttpSpan span, Request request, Response response) {
        try {
            String messageId = extractMessageId(request.getOriginalRequest());
            if (messageId != null) span.getInfo().setMessageId(messageId);
            String tableName = extractTableName(request.getOriginalRequest());
            if (tableName != null) span.getInfo().setResourceName(tableName);
        } catch (Exception e) {
            Logger.error(e, "Failed to parse for DynamoDB request");
        }
    }

    public void parseV2(HttpSpan span, Context.AfterExecution context) {
        try {
            System.out.println("Inside AWS Parser Dynamodb V2");
            if (context.request().getValueForField("TableName", String.class).isPresent()) {
                context.request().getValueForField("TableName", String.class).ifPresent(
                        tableName -> {
                            span.getInfo().setResourceName(tableName);
                            Logger.debug("Got TableName : " + tableName);
                        }
                );
            } else {
                Logger.warn("Failed to extract queueUrl form SQS request");
            }
            span.getInfo().setMessageId(extractMessageIdV2(context.request()));
        } catch (Exception e) {
            Logger.error(e, "Failed to parse for DynamoDB request");
        }
    }

    private String extractMessageId(AmazonWebServiceRequest request) {
        if (request instanceof PutItemRequest) {
            return dynamodbItemToHash(((PutItemRequest) request).getItem());
        } else if (request instanceof UpdateItemRequest) {
            return dynamodbItemToHash(((UpdateItemRequest) request).getKey());
        } else if (request instanceof DeleteItemRequest) {
            return dynamodbItemToHash(((DeleteItemRequest) request).getKey());
        } else if (request instanceof BatchWriteItemRequest) {
            Map<String, List<WriteRequest>> requests =
                    ((BatchWriteItemRequest) request).getRequestItems();
            WriteRequest firstRequest = requests.entrySet().iterator().next().getValue().get(0);
            if (firstRequest.getPutRequest() != null) {
                return dynamodbItemToHash(firstRequest.getPutRequest().getItem());
            } else if (firstRequest.getDeleteRequest() != null) {
                return dynamodbItemToHash(firstRequest.getDeleteRequest().getKey());
            }
        }
        return null;
    }

    private String extractMessageIdV2(SdkRequest request) {
        if (request instanceof software.amazon.awssdk.services.dynamodb.model.PutItemRequest) {
            return dynamodbItemToHashV2(((software.amazon.awssdk.services.dynamodb.model.PutItemRequest) request).item());
        } else if (request instanceof software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest) {
            return dynamodbItemToHashV2(((software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest) request).key());
        } else if (request instanceof software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest) {
            return dynamodbItemToHashV2(((software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest) request).key());
        } else if (request instanceof software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest) {
            Map<String, List<software.amazon.awssdk.services.dynamodb.model.WriteRequest>> requests =
                    ((software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest) request).requestItems();
            software.amazon.awssdk.services.dynamodb.model.WriteRequest firstRequest = requests.entrySet().iterator().next().getValue().get(0);
            if (firstRequest.putRequest() != null) {
                return dynamodbItemToHashV2(firstRequest.putRequest().item());
            } else if (firstRequest.deleteRequest() != null) {
                return dynamodbItemToHashV2(firstRequest.deleteRequest().key());
            }
        }
        return null;
    }

    private String extractTableName(AmazonWebServiceRequest request) {
        if (request instanceof PutItemRequest) {
            return ((PutItemRequest) request).getTableName();
        } else if (request instanceof UpdateItemRequest) {
            return ((UpdateItemRequest) request).getTableName();
        } else if (request instanceof DeleteItemRequest) {
            return ((DeleteItemRequest) request).getTableName();
        } else if (request instanceof BatchWriteItemRequest) {
            Map<String, List<WriteRequest>> requests =
                    ((BatchWriteItemRequest) request).getRequestItems();
            return requests.entrySet().iterator().next().getKey();
        } else if (request instanceof BatchGetItemRequest) {
            Map<String, KeysAndAttributes> requests =
                    ((BatchGetItemRequest) request).getRequestItems();
            return requests.entrySet().iterator().next().getKey();
        } else if (request instanceof GetItemRequest) {
            return ((GetItemRequest) request).getTableName();
        }
        return null;
    }
}
