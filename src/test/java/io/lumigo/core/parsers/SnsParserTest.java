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
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                            + "<PublishResponse xmlns=\"https://sns.amazonaws.com/doc/2010-03-31/\">"
                            + "<PublishResult>"
                            + "<MessageId>567910cd-659e-55d4-8ccb-5aaf14679dc0</MessageId>"
                            + "</PublishResult><ResponseMetadata><RequestId>d74b8436-ae13-5ab4-a9ff-ce54dfea72a0</RequestId>"
                            + "</ResponseMetadata>"
                            + "</PublishResponse>",
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
        assertEquals("567910cd-659e-55d4-8ccb-5aaf14679dc0", span.getInfo().getMessageId());
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
