package io.lumigo.core.parsers.v1;

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

public class KinesisV1Parser implements AwsSdkV1Parser {
    @Override
    public String getParserType() {
        return KinesisV1Parser.class.getName();
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
}
