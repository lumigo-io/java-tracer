package io.lumigo.core.parsers.v2;

import io.lumigo.core.utils.AwsSdkV2Utils;
import io.lumigo.models.HttpSpan;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.services.dynamodb.model.*;

public class DynamoDBV2Parser implements AwsSdkV2Parser {
    @Override
    public String getParserType() {
        return DynamoDBV2Parser.class.getName();
    }

    public void parse(HttpSpan span, Context.AfterExecution context) {
        SdkRequest request = context.request();
        if (context.request().getValueForField("TableName", String.class).isPresent()) {
            context.request()
                    .getValueForField("TableName", String.class)
                    .ifPresent(
                            tableName -> {
                                span.getInfo().setResourceName(tableName);
                                Logger.debug("Parsed TableName : " + tableName);
                            });
        } else if (request instanceof BatchWriteItemRequest
                && ((BatchWriteItemRequest) request).hasRequestItems()) {
            ((BatchWriteItemRequest) request)
                    .requestItems().keySet().stream()
                            .findFirst()
                            .ifPresent(
                                    tableName -> {
                                        span.getInfo().setResourceName(tableName);
                                        Logger.debug("Parsed TableName : " + tableName);
                                    });
        } else if (request instanceof BatchGetItemRequest) {
            ((BatchGetItemRequest) request)
                    .requestItems().keySet().stream()
                            .findFirst()
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
        if (request instanceof PutItemRequest) {
            return AwsSdkV2Utils.calculateItemHash(((PutItemRequest) request).item());
        } else if (request instanceof UpdateItemRequest) {
            return AwsSdkV2Utils.calculateItemHash(((UpdateItemRequest) request).key());
        } else if (request instanceof DeleteItemRequest) {
            return AwsSdkV2Utils.calculateItemHash(((DeleteItemRequest) request).key());
        } else if (request instanceof BatchWriteItemRequest) {
            Map<String, List<WriteRequest>> requests =
                    ((BatchWriteItemRequest) request).requestItems();
            WriteRequest firstRequest = requests.entrySet().iterator().next().getValue().get(0);
            if (firstRequest.putRequest() != null) {
                return AwsSdkV2Utils.calculateItemHash(firstRequest.putRequest().item());
            } else if (firstRequest.deleteRequest() != null) {
                return AwsSdkV2Utils.calculateItemHash(firstRequest.deleteRequest().key());
            }
        }
        return null;
    }
}
