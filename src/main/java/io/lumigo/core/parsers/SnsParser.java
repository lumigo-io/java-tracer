package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.sns.model.PublishResult;
import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class SnsParser implements AwsParser {
    @Override
    public String getParserType() {
        return SnsParser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Request request, Response response) {
        String topicArn = getParameter(request, "TopicArn");
        span.getInfo().setResourceName(topicArn);
        span.getInfo().setTargetArn(topicArn);
        span.getInfo().setMessageId(extractMessageId(response.getAwsResponse()));
    }

    @Override
    public void parseV2(HttpSpan span, Context.AfterExecution context) {
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
        span.getInfo().setMessageId(extractMessageIdV2(context.response()));
    }

    private String extractMessageId(Object response) {
        try {
            if (response instanceof PublishResult) {
                return ((PublishResult) response).getMessageId();
            } else {
                Logger.error("Failed to extract messageId for SNS response");
                return null;
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to extract messageId for SNS response");
            return null;
        }
    }

    private String extractMessageIdV2(SdkResponse response) {
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
