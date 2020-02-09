package io.lumigo.core.network;

import static org.mockito.Mockito.when;

import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.utils.EnvUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
}
