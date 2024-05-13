package io.lumigo.core.parsers.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.lumigo.models.HttpSpan;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

class SqsV2ParserTest {

    private HttpSpan span = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
    SqsV2Parser sqsParser = new SqsV2Parser();

    @Test
    void test_parse_sqs_with_full_details_v2() {
        SendMessageRequest publishRequest =
                SendMessageRequest.builder().queueUrl("queueUrl").build();
        SendMessageResponse messageResponse =
                SendMessageResponse.builder()
                        .messageId("fee47356-6f6a-58c8-96dc-26d8aaa4631a")
                        .build();
        Context.AfterExecution context =
                InterceptorContext.builder()
                        .request(publishRequest)
                        .response(messageResponse)
                        .build();

        sqsParser.safeParse(span, context);

        assertEquals("queueUrl", span.getInfo().getResourceName());
        assertEquals("fee47356-6f6a-58c8-96dc-26d8aaa4631a", span.getInfo().getMessageId());
    }
}
