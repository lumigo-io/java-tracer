package io.lumigo.core.parsers.v2;

import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class SnsV2Parser implements AwsSdkV2Parser {
    @Override
    public String getParserType() {
        return SnsV2Parser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Context.AfterExecution context) {
        if (context.request().getValueForField("TopicArn", String.class).isPresent()) {
            context.request()
                    .getValueForField("TopicArn", String.class)
                    .ifPresent(
                            topicArn -> {
                                Logger.debug("Parsed topicArn : " + topicArn);
                                span.getInfo().setResourceName(topicArn);
                                span.getInfo().setTargetArn(topicArn);
                            });
        } else {
            Logger.warn("Failed to extract topicArn");
        }
        span.getInfo().setMessageId(extractMessageId(context.response()));
    }

    private String extractMessageId(SdkResponse response) {
        try {
            if (response instanceof PublishResponse) {
                return ((PublishResponse) response).messageId();
            } else {
                Logger.error("Failed to extract messageId for SNS response");
                return null;
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to extract messageId for SNS response");
            return null;
        }
    }
}
