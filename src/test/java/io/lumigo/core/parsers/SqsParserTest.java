package io.lumigo.core.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import io.lumigo.models.ContainerHttpSpan;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SqsParserTest {

    private ContainerHttpSpan span = ContainerHttpSpan.builder().build();
    SqsParser sqsParser = new SqsParser();
    @Mock Request request;
    @Mock HttpResponse httpResponse;
    @Mock SendMessageResult sqsResult;
    @Mock SendMessageRequest sqsRequest;
    Response response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        response = new Response(sqsResult, httpResponse);
    }

    @Test
    void test_parse_sqs_with_full_details() {
        when(sqsResult.getMessageId()).thenReturn("fee47356-6f6a-58c8-96dc-26d8aaa4631a");
        when(sqsRequest.getQueueUrl()).thenReturn("queueUrl");
        when(request.getOriginalRequest()).thenReturn(sqsRequest);

        sqsParser.parse(span, request, response);

        assertEquals("queueUrl", span.getResourceName());
        assertEquals("fee47356-6f6a-58c8-96dc-26d8aaa4631a", span.getMessageIds().get(0));
    }

    @Test
    void test_parse_sqs_with_no_data() {
        when(request.getParameters()).thenReturn(new HashMap<>());

        sqsParser.parse(span, request, new Response(null, httpResponse));

        assertNull(span.getResourceName());
        assertNull(span.getMessageIds());
    }

    @Test
    void test_parse_sqs_with_exception() {
        when(sqsResult.getMessageId()).thenThrow(new RuntimeException());
        when(request.getParameters()).thenReturn(new HashMap<>());

        sqsParser.parse(span, request, response);

        assertNull(span.getResourceName());
        assertNull(span.getMessageIds());
    }
}
