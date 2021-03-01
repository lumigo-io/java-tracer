package io.lumigo.core.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.EnvUtil;
import io.lumigo.models.Span;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ReporterTest {

    Reporter reporter = new Reporter();

    @Mock EnvUtil envUtil;
    private Map<String, String> env = new HashMap<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        createMockedEnv();
        Configuration.getInstance().setEnvUtil(envUtil);
    }

    private void createMockedEnv() {
        addEnvMock(Configuration.TRACER_HOST_KEY, "google.com");
        when(envUtil.getEnv()).thenReturn(env);
    }

    private void addEnvMock(String key, String value) {
        env.put(key, value);
        when(envUtil.getEnv(key)).thenReturn(value);
    }

    @Test
    void reportSpans() throws IOException {
        addEnvMock("LAMBDA_RUNTIME_DIR", "/");
        long l = reporter.reportSpans(Span.builder().build(), 900_000);
        assertTrue(l > 0);
    }

    @Test
    void reportSpanDropBySize() throws IOException {
        addEnvMock("LAMBDA_RUNTIME_DIR", "/");
        long l = reporter.reportSpans(Span.builder().build(), 0);
        assertEquals(0, l);
    }

    @Test
    void reportSpans_not_aws_run() throws IOException {
        env.remove("LAMBDA_RUNTIME_DIR");
        long l = reporter.reportSpans(Span.builder().build(), 900_000);
        assertEquals(0, l);
    }
}
