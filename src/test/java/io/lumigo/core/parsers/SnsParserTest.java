package io.lumigo.core.parsers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import io.lumigo.models.HttpSpan;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SnsParserTest {

    private HttpSpan span = HttpSpan.builder().info(HttpSpan.Info.builder().build()).build();
    SnsParser SnsParser = new SnsParser();
    @Mock Request request;
    @Mock HttpResponse httpResponse;
    Response response =
            new Response(
                    "{\"sdkResponseMetadata\":{\"requestId\":\"57a7fbab-b6f3-5eb1-acbf-ae25733d6563\"},\"sdkHttpMetadata\":{\"httpHeaders\":{\"Content-Length\":\"294\",\"Content-Type\":\"text/xml\",\"Date\":\"Thu, 27 Jun 2019 13:24:29 GMT\",\"x-amzn-RequestId\":\"57a7fbab-b6f3-5eb1-acbf-ae25733d6563\"},\"httpStatusCode\":200},\"messageId\":\"fee47356-6f6a-58c8-96dc-26d8aaa4631a\"}",
                    httpResponse);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void test_parse_sns_with_full_details() {
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("TopicArn", Arrays.asList("topic"));
        when(request.getParameters()).thenReturn(parameters);

        SnsParser.parse(span, request, response);

        assertEquals("topic", span.getInfo().getResourceName());
        assertEquals("topic", span.getInfo().getTargetArn());
        assertEquals("fee47356-6f6a-58c8-96dc-26d8aaa4631a", span.getInfo().getMessageId());
    }

    @Test
    void test_parse_sns_with_no_data() {
        when(request.getParameters()).thenReturn(new HashMap<>());

        SnsParser.parse(span, request, new Response(null, httpResponse));

        assertNull(span.getInfo().getResourceName());
        assertNull(span.getInfo().getTargetArn());
        assertNull(span.getInfo().getMessageId());
    }
}
