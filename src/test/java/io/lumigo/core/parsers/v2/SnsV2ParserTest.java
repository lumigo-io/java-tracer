package io.lumigo.core.parsers.v2;

import static org.junit.jupiter.api.Assertions.*;

import io.lumigo.models.HttpSpan;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

class SnsV2ParserTest {

    private HttpSpan span = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
    SnsV2Parser SnsParser = new SnsV2Parser();

    @Test
    void test_parse_sns_with_full_details_v2() {
        PublishRequest publishRequest = PublishRequest.builder().topicArn("topic").build();
        PublishResponse publishResponse =
                PublishResponse.builder().messageId("fee47356-6f6a-58c8-96dc-26d8aaa4631a").build();
        Context.AfterExecution context =
                InterceptorContext.builder()
                        .request(publishRequest)
                        .response(publishResponse)
                        .build();

        SnsParser.safeParse(span, context);

        assertEquals("topic", span.getInfo().getResourceName());
        assertEquals("topic", span.getInfo().getTargetArn());
        assertEquals("fee47356-6f6a-58c8-96dc-26d8aaa4631a", span.getInfo().getMessageId());
    }
}
