package io.lumigo.core.parsers;

import static io.lumigo.core.utils.StringUtils.buildMd5Hash;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.dynamodbv2.model.*;
import io.lumigo.models.HttpSpan;
import java.util.List;
import java.util.Map;
import org.pmw.tinylog.Logger;

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

    private String extractMessageId(AmazonWebServiceRequest request) {

        if (request instanceof PutItemRequest) {
            return buildMd5Hash(((PutItemRequest) request).getItem());
        } else if (request instanceof UpdateItemRequest) {
            return buildMd5Hash(((UpdateItemRequest) request).getKey());
        } else if (request instanceof DeleteItemRequest) {
            return buildMd5Hash(((DeleteItemRequest) request).getKey());
        } else if (request instanceof BatchWriteItemRequest) {
            Map<String, List<WriteRequest>> requests =
                    ((BatchWriteItemRequest) request).getRequestItems();
            WriteRequest firstRequest = requests.entrySet().iterator().next().getValue().get(0);
            if (firstRequest.getPutRequest() != null) {
                return buildMd5Hash(firstRequest.getPutRequest().getItem());
            } else if (firstRequest.getDeleteRequest() != null) {
                return buildMd5Hash(firstRequest.getDeleteRequest().getKey());
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
