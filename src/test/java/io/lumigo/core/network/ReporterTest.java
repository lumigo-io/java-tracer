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
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ReporterTest {

    Reporter reporter = new Reporter();

    @Mock EnvUtil envUtil;
    @Mock OkHttpClient client;
    @Mock Call call;
    private Map<String, String> env = new HashMap<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        createMockedEnv();
        Configuration.getInstance().setEnvUtil(envUtil);
        when(client.newCall(any(Request.class))).thenReturn(call);
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

        ArgumentCaptor<Request> argumentCaptorRequest = ArgumentCaptor.forClass(Request.class);
        verify(client, Mockito.times(1)).newCall(argumentCaptorRequest.capture());
    }
}
