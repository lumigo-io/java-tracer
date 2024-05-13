package io.lumigo.core.parsers.v2;

import io.lumigo.core.utils.AwsSdkV2Utils;
import io.lumigo.core.utils.JsonUtils;
import io.lumigo.core.utils.StringUtils;
import io.lumigo.models.HttpSpan;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;

public class DynamoDBV2Parser implements AwsSdkV2Parser {
    @Override
    public String getParserType() {
        return DynamoDBV2Parser.class.getName();
    }

    public void parse(HttpSpan span, Context.AfterExecution context) {
        if (context.request().getValueForField("TableName", String.class).isPresent()) {
            context.request()
                    .getValueForField("TableName", String.class)
                    .ifPresent(
                            tableName -> {
                                span.getInfo().setResourceName(tableName);
                                Logger.debug("Parsed TableName : " + tableName);
                            });
        } else {
            Logger.warn("Failed to extract TableName form DynamoDB request");
        }
        span.getInfo().setMessageId(extractMessageId(context.request()));
    }

    private String extractMessageId(SdkRequest request) {
        if (request instanceof software.amazon.awssdk.services.dynamodb.model.PutItemRequest) {
            return calculateV2ItemHash(
                    ((software.amazon.awssdk.services.dynamodb.model.PutItemRequest) request)
                            .item());
        } else if (request
                instanceof software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest) {
            return calculateV2ItemHash(
                    ((software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest) request)
                            .key());
        } else if (request
                instanceof software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest) {
            return calculateV2ItemHash(
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
                return calculateV2ItemHash(firstRequest.putRequest().item());
            } else if (firstRequest.deleteRequest() != null) {
                return calculateV2ItemHash(firstRequest.deleteRequest().key());
            }
        }
        return null;
    }

    private String calculateV2ItemHash(
            Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> item) {
        Map<String, Object> simpleMap = AwsSdkV2Utils.convertAttributeMapToSimpleMap(item);
        return StringUtils.buildMd5Hash(JsonUtils.getObjectAsJsonString(simpleMap));
    }
}
