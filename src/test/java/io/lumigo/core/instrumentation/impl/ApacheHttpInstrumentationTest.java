package io.lumigo.core.instrumentation.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.handlers.LumigoConfiguration;
import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApacheHttpInstrumentationTest {

    @Mock HttpUriRequest request;
    @Mock HttpResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        initConfiguration();
    }

    @Test
    public void handling_enter_valid_request() {
        when(request.getURI()).thenReturn(URI.create("https://not.lumigo.host/api/spans"));

        ApacheHttpInstrumentation.ApacheHttpAdvice.methodEnter(request);

        assertNotNull(
                ApacheHttpInstrumentation.ApacheHttpAdvice.startTimeMap.get(request.hashCode()));
    }

    @Test
    public void handling_enter_lumigo_internal_request() {
        when(request.getURI()).thenReturn(URI.create("https://lumigo.io/api/spans"));

        ApacheHttpInstrumentation.ApacheHttpAdvice.methodEnter(request);

        assertNull(ApacheHttpInstrumentation.ApacheHttpAdvice.startTimeMap.get(request.hashCode()));
    }

    @Test
    public void handling_enter_exception() {
        when(request.getURI()).thenThrow(new RuntimeException());

        ApacheHttpInstrumentation.ApacheHttpAdvice.methodEnter(request);

        assertNull(ApacheHttpInstrumentation.ApacheHttpAdvice.startTimeMap.get(request.hashCode()));
    }

    @Test
    public void handling_exit_response_lumigo_internal_request() {
        when(request.getURI()).thenReturn(URI.create("https://lumigo.io/api/spans"));

        ApacheHttpInstrumentation.ApacheHttpAdvice.methodExit(request, response);

        assertNull(ApacheHttpInstrumentation.ApacheHttpAdvice.handled.get(request.hashCode()));
    }

    @Test
    public void handling_exit_response_unknown_exception() {
        when(request.getURI()).thenThrow(new RuntimeException());

        ApacheHttpInstrumentation.ApacheHttpAdvice.methodExit(request, response);

        assertNull(ApacheHttpInstrumentation.ApacheHttpAdvice.handled.get(request.hashCode()));
    }

    @Test
    public void handling_exit_response_already_handled() {
        when(request.getURI()).thenReturn(URI.create("https://not.lumigo.host/api/spans"));
        ApacheHttpInstrumentation.ApacheHttpAdvice.handled.put(request.hashCode(), true);

        ApacheHttpInstrumentation.ApacheHttpAdvice.methodExit(request, response);

        assertNotNull(ApacheHttpInstrumentation.ApacheHttpAdvice.handled.get(request.hashCode()));
    }

    @Test
    public void check_typeMatcher() {
        assertNotNull(new ApacheHttpInstrumentation().getTypeMatcher());
    }

    @Test
    public void check_transformer() {
        assertNotNull(new ApacheHttpInstrumentation().getTransformer());
    }

    private void initConfiguration() {
        Configuration.init(LumigoConfiguration.builder().edgeHost("lumigo.io").build());
    }
}
