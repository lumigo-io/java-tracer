package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;

public class SqsParser implements AwsParser {
    @Override
    public void parse(HttpSpan span, Request request, Response response) {
        if (request.getOriginalRequest() instanceof SendMessageRequest) {
            String queueUrl = ((SendMessageRequest) request.getOriginalRequest()).getQueueUrl();
            span.getInfo().setResourceName(queueUrl);
            Logger.debug("Got queueUrl : " + queueUrl);
        } else {
            Logger.error("Failed to extract queueUrl form SQS request");
        }
        span.getInfo().setMessageId(extractMessageId(response.getAwsResponse()));
    }

    private String extractMessageId(Object response) {
        try {
            if (response instanceof SendMessageResult) {
                String messageId = ((SendMessageResult) response).getMessageId();
                Logger.debug("Got getMessageId : " + messageId);
                return messageId;
            } else {
                Logger.error("Failed to extract messageId for SQS response");
                return null;
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to extract messageId for SQS response");
            return null;
        }
    }
}
