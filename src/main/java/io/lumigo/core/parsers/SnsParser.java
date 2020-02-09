package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.sns.model.PublishResult;
import io.lumigo.models.ContainerHttpSpan;
import java.util.Collections;
import org.pmw.tinylog.Logger;

public class SnsParser implements AwsParser {
    @Override
    public void parse(ContainerHttpSpan span, Request request, Response response) {
        String topicArn = getParameter(request, "TopicArn");
        span.setResourceName(topicArn);
        span.setTargetArn(topicArn);
        String messageId = extractMessageId(response.getAwsResponse());
        if (messageId != null) {
            span.setMessageIds(Collections.singletonList(messageId));
        }
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
