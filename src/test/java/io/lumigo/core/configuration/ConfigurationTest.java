package io.lumigo.core.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.lumigo.core.utils.EnvUtil;
import io.lumigo.handlers.LumigoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmw.tinylog.Level;

class ConfigurationTest {

    @Mock private EnvUtil envUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        Configuration.getInstance().init(null);
        when(envUtil.getBooleanEnv(any(), any())).thenCallRealMethod();
    }

    @Test
    void getLumigoToken_inline() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.TOKEN_KEY)).thenReturn("abcd");
        Configuration.getInstance().init(LumigoConfiguration.builder().token("1234").build());

        assertEquals("1234", Configuration.getInstance().getLumigoToken());
    }

    @Test
    void getLumigoToken_env() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.TOKEN_KEY)).thenReturn("abcd");

        assertEquals("abcd", Configuration.getInstance().getLumigoToken());
    }

    @Test
    void getLumigoEdge_inline() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.TRACER_HOST_KEY)).thenReturn("defaultUrl");
        Configuration.getInstance()
                .init(LumigoConfiguration.builder().edgeHost("inlineUrl").build());

        assertEquals("https://inlineUrl/api/spans", Configuration.getInstance().getLumigoEdge());
    }

    @Test
    void getLumigoEdge_env() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.TRACER_HOST_KEY)).thenReturn("defaultUrl");

        assertEquals("https://defaultUrl/api/spans", Configuration.getInstance().getLumigoEdge());
    }

    @Test
    void getLogLevel_env() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.DEBUG_KEY)).thenReturn("true");

        assertEquals(Level.DEBUG, Configuration.getInstance().getLogLevel());
    }

    @Test
    void getLogLevel_default() {
        Configuration.getInstance().setEnvUtil(envUtil);
        assertEquals(Level.OFF, Configuration.getInstance().getLogLevel());
    }

    @Test
    void isAwsEnvironment_true() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv("LAMBDA_RUNTIME_DIR")).thenReturn("value");

        assertTrue(Configuration.getInstance().isAwsEnvironment());
    }

    @Test
    void isAwsEnvironment_false() {
        Configuration.getInstance().setEnvUtil(envUtil);
        assertFalse(Configuration.getInstance().isAwsEnvironment());
    }

    @Test
    void isAmazonHost_true() {
        assertTrue(Configuration.getInstance().isAwsHost("https://sns.amazonaws.com"));
    }

    @Test
    void isAmazonHost_false() {
        assertFalse(Configuration.getInstance().isAwsHost("https://google.com"));
    }

    @Test
    void isLumigoVerboseMode_default_true() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(any())).thenReturn(null);

        assertTrue(Configuration.getInstance().isLumigoVerboseMode());
    }

    @Test
    void isLumigoVerboseMode_env_false() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.LUMIGO_VERBOSE)).thenReturn("false");

        assertFalse(Configuration.getInstance().isLumigoVerboseMode());
    }

    @Test
    void isLumigoVerboseMode_inline_false() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.LUMIGO_VERBOSE)).thenReturn("false");
        Configuration.getInstance().init(LumigoConfiguration.builder().verbose(true).build());

        assertTrue(Configuration.getInstance().isLumigoVerboseMode());
    }

    @Test
    void timeout_from_env() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.REPORTER_TIMEOUT)).thenReturn("5000");
        assertEquals(5000, Configuration.getInstance().getLumigoTimeout().toMillis());

        assertTrue(Configuration.getInstance().isLumigoVerboseMode());
    }

    @Test
    void timeout_default() {
        Configuration.getInstance().setEnvUtil(envUtil);
        assertEquals(3000, Configuration.getInstance().getLumigoTimeout().toMillis());

        assertTrue(Configuration.getInstance().isLumigoVerboseMode());
    }

    @Test
    void kill_switch_on() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getEnv(Configuration.LUMIGO_KILL_SWITCH)).thenReturn("true");
        assertTrue(Configuration.getInstance().isKillingSwitchActivated());
    }

    @Test
    void kill_switch_default_off() {
        Configuration.getInstance().setEnvUtil(envUtil);
        assertFalse(Configuration.getInstance().isKillingSwitchActivated());
    }

    @Test
    void version() {
        assertNotNull(Configuration.getInstance().getLumigoTracerVersion());
    }

    @Test
    void maxSpanFieldSize() {
        Configuration.getInstance().setEnvUtil(envUtil);
        when(envUtil.getIntegerEnv(Configuration.LUMIGO_MAX_ENTRY_SIZE, 1024)).thenReturn(1024);
        assertEquals(1024, Configuration.getInstance().maxSpanFieldSize());
    }
}
