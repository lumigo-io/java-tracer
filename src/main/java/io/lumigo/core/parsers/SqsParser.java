package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsRequest;

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

    @Override
    public void parseV2(HttpSpan span, Context.AfterExecution context) {
        System.out.println("Inside AWS Parser SQS V2");
        if (context.request().getValueForField("QueueUrl", String.class).isPresent()) {
            context.request().getValueForField("QueueUrl", String.class).ifPresent(
                    queueUrl -> {
                        span.getInfo().setResourceName(queueUrl);
                        Logger.debug("Got queueUrl : " + queueUrl);
                    }
            );
        } else {
            Logger.warn("Failed to extract queueUrl form SQS request");
        }
        span.getInfo().setMessageId(extractMessageIdV2(context.response()));
    }

    private String extractMessageId(Object response) {
        try {
            if (response instanceof SendMessageResult) {
                String messageId = ((SendMessageResult) response).getMessageId();
                Logger.debug("Got messageId : " + messageId);
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

    private String extractMessageIdV2(SdkResponse response) {
        try {
            if (response instanceof SendMessageResponse) {
                String messageId = ((SendMessageResponse) response).messageId();
                Logger.debug("Got messageId : " + messageId);
                return messageId;
            }
            else {
                Logger.error("Failed to extract messageId for SQS response");
                return null;
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to extract messageId for SQS response");
            return null;
        }
    }
}
