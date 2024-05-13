package io.lumigo.core.parsers.v2;

import io.lumigo.models.HttpSpan;
import java.util.LinkedList;
import java.util.List;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;

public class KinesisV2Parser implements AwsSdkV2Parser {
    @Override
    public String getParserType() {
        return KinesisV2Parser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Context.AfterExecution context) {
        if (context.request().getValueForField("StreamName", String.class).isPresent()) {
            context.request()
                    .getValueForField("StreamName", String.class)
                    .ifPresent(
                            streamName -> {
                                span.getInfo().setResourceName(streamName);
                                Logger.debug("Parsed StreamName : " + streamName);
                            });
        }
        List<String> messageIds = extractMessageIds(context.response());
        if (!messageIds.isEmpty()) span.getInfo().setMessageIds(messageIds);
    }

    private List<String> extractMessageIds(Object response) {
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
