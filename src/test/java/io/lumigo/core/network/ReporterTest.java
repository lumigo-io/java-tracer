package io.lumigo.core.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.EnvUtil;
import io.lumigo.models.Span;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ReporterTest {

    Reporter reporter = new Reporter();

    @Mock EnvUtil envUtil;
    @Mock HttpClient client;
    @Mock HttpResponse response;
    private Map<String, String> env = new HashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        createMockedEnv();
        Configuration.getInstance().setEnvUtil(envUtil);
        when(client.execute(any(HttpPost.class))).thenReturn(response);
        reporter.setClient(client);
    }

    private void createMockedEnv() {
        addEnvMock("LAMBDA_RUNTIME_DIR", "/");
        when(envUtil.getEnv()).thenReturn(env);
    }

    private void addEnvMock(String key, String value) {
        env.put(key, value);
        when(envUtil.getEnv(key)).thenReturn(value);
    }

    @Test
    void reportSpans() throws IOException {
        reporter.reportSpans(Span.builder().build());

        ArgumentCaptor<HttpPost> argumentCaptorRequest = ArgumentCaptor.forClass(HttpPost.class);
        verify(client, Mockito.times(1)).execute(argumentCaptorRequest.capture());
    }
}
