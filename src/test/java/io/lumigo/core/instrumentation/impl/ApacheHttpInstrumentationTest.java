package io.lumigo.core.instrumentation.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.handlers.LumigoConfiguration;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApacheHttpInstrumentationTest {

    @Mock HttpUriRequest request;
    @Mock HttpResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        initSpansContainer();
        initConfiguration();
    }

    @Test
    public void handling_enter_valid_request() {
        when(request.getURI()).thenReturn(URI.create("https://not.lumigo.host/api/spans"));

        ApacheHttpInstrumentation.AmazonHttpClientAdvice.methodEnter(request);

        assertNotNull(
                ApacheHttpInstrumentation.AmazonHttpClientAdvice.startTimeMap.get(
                        request.hashCode()));
        verify(request, times(1)).setHeader(eq("X-Amzn-Trace-Id"), any());
    }

    @Test
    public void handling_enter_lumigo_internal_request() {
        when(request.getURI()).thenReturn(URI.create("https://lumigo.io/api/spans"));

        ApacheHttpInstrumentation.AmazonHttpClientAdvice.methodEnter(request);

        assertNull(
                ApacheHttpInstrumentation.AmazonHttpClientAdvice.startTimeMap.get(
                        request.hashCode()));
        verify(request, times(0)).setHeader(eq("X-Amzn-Trace-Id"), any());
    }

    @Test
    public void handling_enter_exception() {
        when(request.getURI()).thenThrow(new RuntimeException());

        ApacheHttpInstrumentation.AmazonHttpClientAdvice.methodEnter(request);

        assertNull(
                ApacheHttpInstrumentation.AmazonHttpClientAdvice.startTimeMap.get(
                        request.hashCode()));
        verify(request, times(0)).setHeader(eq("X-Amzn-Trace-Id"), any());
    }

    @Test
    public void handling_exit_response_lumigo_internal_request() {
        when(request.getURI()).thenReturn(URI.create("https://lumigo.io/api/spans"));

        ApacheHttpInstrumentation.AmazonHttpClientAdvice.methodExit(request, response);

        assertNull(
                ApacheHttpInstrumentation.AmazonHttpClientAdvice.handled.get(request.hashCode()));
    }

    @Test
    public void handling_exit_response_unknown_exception() {
        when(request.getURI()).thenThrow(new RuntimeException());

        ApacheHttpInstrumentation.AmazonHttpClientAdvice.methodExit(request, response);

        assertNull(
                ApacheHttpInstrumentation.AmazonHttpClientAdvice.handled.get(request.hashCode()));
    }

    @Test
    public void handling_exit_response_already_handled() {
        when(request.getURI()).thenReturn(URI.create("https://not.lumigo.host/api/spans"));
        ApacheHttpInstrumentation.AmazonHttpClientAdvice.handled.put(request.hashCode(), true);

        ApacheHttpInstrumentation.AmazonHttpClientAdvice.methodExit(request, response);

        assertNotNull(
                ApacheHttpInstrumentation.AmazonHttpClientAdvice.handled.get(request.hashCode()));
    }

    @Disabled
    @Test
    public void handling_exit_response_create_new_span() throws Exception {
        when(request.getURI()).thenReturn(URI.create("https://not.lumigo.host/api/spans"));

        ApacheHttpInstrumentation.AmazonHttpClientAdvice.methodExit(request, response);

        assertEquals(1, SpansContainer.getInstance().getHttpSpans().size());
        assertNotNull(
                ApacheHttpInstrumentation.AmazonHttpClientAdvice.handled.get(request.hashCode()));
    }

    @Test
    public void check_typeMatcher() {
        assertNotNull(new ApacheHttpInstrumentation().getTypeMatcher());
    }

    @Test
    public void check_transformer() {
        assertNotNull(new ApacheHttpInstrumentation().getTransformer());
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
