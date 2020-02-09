package io.lumigo.core.parsers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.sns.model.PublishResult;
import io.lumigo.models.ContainerHttpSpan;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SnsParserTest {

    private ContainerHttpSpan span = ContainerHttpSpan.builder().build();
    SnsParser SnsParser = new SnsParser();
    @Mock Request request;
    @Mock HttpResponse httpResponse;
    @Mock PublishResult snsResult;
    Response response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        response = new Response(snsResult, httpResponse);
    }

    @Test
    void test_parse_sns_with_full_details() {
        when(snsResult.getMessageId()).thenReturn("fee47356-6f6a-58c8-96dc-26d8aaa4631a");
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("TopicArn", Arrays.asList("topic"));
        when(request.getParameters()).thenReturn(parameters);

        SnsParser.parse(span, request, response);

        assertEquals("topic", span.getResourceName());
        assertEquals("topic", span.getTargetArn());
        assertEquals("fee47356-6f6a-58c8-96dc-26d8aaa4631a", span.getMessageIds().get(0));
    }

    @Test
    void test_parse_sns_with_no_data() {
        when(request.getParameters()).thenReturn(new HashMap<>());

        SnsParser.parse(span, request, new Response(null, httpResponse));

        assertNull(span.getResourceName());
        assertNull(span.getTargetArn());
        assertNull(span.getMessageIds());
    }

    @Test
    void test_parse_sns_with_exception() {
        when(snsResult.getMessageId()).thenThrow(new RuntimeException());
        when(request.getParameters()).thenReturn(new HashMap<>());

        SnsParser.parse(span, request, response);

        assertNull(span.getResourceName());
        assertNull(span.getTargetArn());
        assertNull(span.getMessageIds());
    }
}
