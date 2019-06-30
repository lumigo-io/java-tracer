package io.lumigo.core.instrumentation.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.handlers.LumigoConfiguration;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AmazonHttpClientInstrumentationTest {
    @Mock Request request;
    @Mock HttpResponse httpResponse;
    Response response = new Response("response", httpResponse);
    @Mock Map<String, String> headers;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        initSpansContainer();
        initConfiguration();
        when(request.getHeaders()).thenReturn(headers);
    }

    @Test
    public void handling_enter_valid_request() {
        when(request.getEndpoint()).thenReturn(URI.create("https://sns.aws.com"));

        AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.methodEnter(request);

        assertNotNull(
                AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.startTimeMap.get(
                        request.hashCode()));
        verify(headers, times(1)).put(eq("X-Amzn-Trace-Id"), any());
    }

    @Test
    public void handling_enter_exception() {
        when(request.getHeaders()).thenThrow(new RuntimeException());

        AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.methodEnter(request);

        assertNull(
                AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.startTimeMap.get(
                        request.hashCode()));
        verify(headers, times(0)).put(eq("X-Amzn-Trace-Id"), any());
    }

    @Test
    public void handling_exit_response_lumigo_internal_request() {
        when(request.getEndpoint()).thenReturn(URI.create("https://lumigo.io/api/spans"));

        AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.methodExit(request, response);

        assertNull(
                AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.handled.get(
                        request.hashCode()));
    }

    @Test
    public void handling_exit_response_unknown_exception() {
        when(request.getEndpoint()).thenThrow(new RuntimeException());

        AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.methodExit(request, response);

        assertNull(
                AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.handled.get(
                        request.hashCode()));
    }

    @Test
    public void handling_exit_response_already_handled() {
        when(request.getEndpoint()).thenReturn(URI.create("https://not.lumigo.host/api/spans"));
        AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.handled.put(
                request.hashCode(), true);

        AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.methodExit(request, response);

        assertNull(
                AmazonHttpClientInstrumentation.AmazonHttpClientAdvice.handled.get(
                        request.hashCode()));
    }

    @Test
    public void check_typeMatcher() {
        assertNotNull(new AmazonHttpClientInstrumentation().getTypeMatcher());
    }

    @Test
    public void check_transformer() {
        assertNotNull(new AmazonHttpClientInstrumentation().getTransformer());
    }

    private void initSpansContainer() {
        Map<String, String> env = new HashMap<>();
        env.put("_X_AMZN_TRACE_ID", "Root=1-2-3;Another=456;Bla=789");
        try {
            SpansContainer.getInstance().clear();
            SpansContainer.getInstance().init(env, null, null, null);
        } catch (Exception e) {
        }
    }

    private void initConfiguration() {
        Configuration.init(LumigoConfiguration.builder().edgeHost("lumigo.io").build());
    }
}
