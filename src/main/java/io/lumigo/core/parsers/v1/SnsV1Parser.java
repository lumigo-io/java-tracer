package io.lumigo.core.parsers.v1;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.sns.model.PublishResult;
import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;

public class SnsV1Parser implements AwsSdkV1Parser {
    @Override
    public String getParserType() {
        return SnsV1Parser.class.getName();
    }

    @Override
    public void parse(HttpSpan span, Request request, Response response) {
        String topicArn = getParameter(request, "TopicArn");
        span.getInfo().setResourceName(topicArn);
        span.getInfo().setTargetArn(topicArn);
        span.getInfo().setMessageId(extractMessageId(response.getAwsResponse()));
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
}
