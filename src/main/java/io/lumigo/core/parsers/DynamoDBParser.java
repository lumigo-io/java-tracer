package io.lumigo.core.parsers;

import static io.lumigo.core.utils.StringUtils.dynamodbItemToHash;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.dynamodbv2.model.*;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.HttpSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;

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
                context.request()
                        .getValueForField("TableName", String.class)
                        .ifPresent(
                                tableName -> {
                                    span.getInfo().setResourceName(tableName);
                                    Logger.debug("Got TableName : " + tableName);
                                });
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
        if (request.getValueForField("Key", String.class).isPresent()) {
            String key = request.getValueForField("Key", String.class).get();
            System.out.println("Extracted key: " + key);
            return key;
        }

        if (request instanceof software.amazon.awssdk.services.dynamodb.model.PutItemRequest) {
            return calculateItemHash(
                    ((software.amazon.awssdk.services.dynamodb.model.PutItemRequest) request)
                            .item());
        } else if (request
                instanceof software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest) {
            return calculateItemHash(
                    ((software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest) request)
                            .key());
        } else if (request
                instanceof software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest) {
            return calculateItemHash(
                    ((software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest) request)
                            .key());
        } else if (request
                instanceof software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest) {
            Map<String, List<software.amazon.awssdk.services.dynamodb.model.WriteRequest>>
                    requests =
                            ((software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest)
                                            request)
                                    .requestItems();
            software.amazon.awssdk.services.dynamodb.model.WriteRequest firstRequest =
                    requests.entrySet().iterator().next().getValue().get(0);
            if (firstRequest.putRequest() != null) {
                return calculateItemHash(firstRequest.putRequest().item());
            } else if (firstRequest.deleteRequest() != null) {
                return calculateItemHash(firstRequest.deleteRequest().key());
            }
        }
        return null;
    }

    private String calculateItemHash(Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item) {
        String calculatedHash = StringUtils.buildMd5Hash(JsonUtils.getObjectAsJsonString(convertToSimpleMap(item)));
        System.out.println("calculated hash: " + calculatedHash);
        return calculatedHash;
    }

    // Helper method to convert from Map<String, AttributeValue> to a Map<String, Object>
    private static Map<String, Object> convertToSimpleMap(Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> attributeValueMap) {
        Map<String, Object> simpleMap = new HashMap<>();
        attributeValueMap.forEach((key, value) -> simpleMap.put(key, attributeValueToObject(value)));
        return simpleMap;
    }

    // Convert AttributeValue to Object
    private static Object attributeValueToObject(software.amazon.awssdk.services.dynamodb.model.AttributeValue value) {
        // Handle each type accordingly
        if (value.s() != null) {
            return value.s();
        } else if (value.n() != null) {
            return value.n();
        } else if (value.bool() != null) {
            return value.bool();
        } else if (value.m() != null) {
            return convertToSimpleMap(value.m());
        } else if (value.l() != null) {
            List<Object> list = new ArrayList<>();
            for (software.amazon.awssdk.services.dynamodb.model.AttributeValue v : value.l()) {
                list.add(attributeValueToObject(v));
            }
            return list;
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
