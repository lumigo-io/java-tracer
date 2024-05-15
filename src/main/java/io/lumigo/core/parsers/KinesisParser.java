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
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

public class KinesisParser implements AwsParser {
    @Override
    public void parse(HttpSpan span, Request request, Response response) {
        try {
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
            if (messageIds.size() > 0) span.getInfo().setMessageIds(messageIds);
        } catch (Exception e) {
            Logger.error(e, "Failed to extract parse for Kinesis request");
        }
    }

    @Override
    public void parseV2(HttpSpan span, SdkHttpFullRequest request, RequestExecutionContext context, SdkHttpFullResponse response) {

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
