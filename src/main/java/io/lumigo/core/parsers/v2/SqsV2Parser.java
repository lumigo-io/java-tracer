package io.lumigo.core.parsers.v2;

import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class SqsV2Parser implements AwsSdkV2Parser {
    @Override
    public String getParserType() {
        return SqsV2Parser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Context.AfterExecution context) {
        if (context.request().getValueForField("QueueUrl", String.class).isPresent()) {
            context.request()
                    .getValueForField("QueueUrl", String.class)
                    .ifPresent(
                            queueUrl -> {
                                span.getInfo().setResourceName(queueUrl);
                                Logger.debug("Parsed queueUrl : " + queueUrl);
                            });
        } else {
            Logger.warn("Failed to extract queueUrl form SQS request");
        }
        span.getInfo().setMessageId(extractMessageId(context.response()));
    }

    private String extractMessageId(SdkResponse response) {
        try {
            if (response instanceof SendMessageResponse) {
                return ((SendMessageResponse) response).messageId();
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
