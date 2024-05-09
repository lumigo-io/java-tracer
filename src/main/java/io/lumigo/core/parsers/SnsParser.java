package io.lumigo.core.parsers;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.services.sns.model.PublishResult;
import io.lumigo.models.HttpSpan;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;

public class SnsParser implements AwsParser {
    @Override
    public void parse(HttpSpan span, Request request, Response response) {
        String topicArn = getParameter(request, "TopicArn");
        span.getInfo().setResourceName(topicArn);
        span.getInfo().setTargetArn(topicArn);
        span.getInfo().setMessageId(extractMessageId(response.getAwsResponse()));
    }

    @Override
    public void parseV2(HttpSpan span, SdkHttpFullRequest request, RequestExecutionContext context, SdkHttpFullResponse response) {

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
