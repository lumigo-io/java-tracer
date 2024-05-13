package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import io.lumigo.models.HttpSpan;
import java.util.LinkedList;
import java.util.List;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;

public class KinesisParser implements AwsParser {
    @Override
    public String getParserType() {
        return KinesisParser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Request request, Response response) {
        if (request.getOriginalRequest() instanceof PutRecordRequest) {
            span.getInfo()
                    .setResourceName(
                            ((PutRecordRequest) request.getOriginalRequest()).getStreamName());
        }
        if (request.getOriginalRequest() instanceof PutRecordsRequest) {
            span.getInfo()
                    .setResourceName(
                            ((PutRecordsRequest) request.getOriginalRequest()).getStreamName());
        }
        List<String> messageIds = extractMessageIds(response.getAwsResponse());
        if (!messageIds.isEmpty()) span.getInfo().setMessageIds(messageIds);
    }

    @Override
    public void parseV2(HttpSpan span, Context.AfterExecution context) {
        if (context.request().getValueForField("StreamName", String.class).isPresent()) {
            context.request()
                    .getValueForField("StreamName", String.class)
                    .ifPresent(
                            streamName -> {
                                span.getInfo().setResourceName(streamName);
                                Logger.debug("Parsed StreamName : " + streamName);
                            });
        }
        List<String> messageIds = extractMessageIdsV2(context.response());
        if (!messageIds.isEmpty()) span.getInfo().setMessageIds(messageIds);
    }

    private List<String> extractMessageIds(Object response) {
        List<String> result = new LinkedList<>();
        if (response instanceof PutRecordsResult) {
            ((PutRecordsResult) response)
                    .getRecords()
                    .forEach(
                            putRecordsResultEntry ->
                                    result.add(putRecordsResultEntry.getSequenceNumber()));
            return result;
        }
        if (response instanceof PutRecordResult) {
            result.add(((PutRecordResult) response).getSequenceNumber());
            return result;
        }
        Logger.error("Failed to extract messageIds for Kinesis response");
        return result;
    }

    private List<String> extractMessageIdsV2(Object response) {
        List<String> messageIds = new LinkedList<>();
        if (response instanceof PutRecordsResponse) {
            ((PutRecordsResponse) response)
                    .records()
                    .forEach(
                            putRecordsResultEntry ->
                                    messageIds.add(putRecordsResultEntry.sequenceNumber()));
            return messageIds;
        }
        if (response instanceof PutRecordResponse) {
            messageIds.add(((PutRecordResponse) response).sequenceNumber());
            return messageIds;
        }
        Logger.error("Failed to extract messageIds for Kinesis response");
        return messageIds;
    }
}
