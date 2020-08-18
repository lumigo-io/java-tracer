package io.lumigo.core.configuration;

import io.lumigo.core.instrumentation.agent.Installer;
import io.lumigo.core.utils.EnvUtil;
import io.lumigo.handlers.LumigoConfiguration;
import java.time.Duration;
import java.util.Locale;
import lombok.Setter;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.ConsoleWriter;

public class Configuration {
    private static final String EDGE_PREFIX = "https://";
    private static final String EDGE_DEFAULT_URL = "%s.lumigo-tracer-edge.golumigo.com";
    private static final String EDGE_SUFFIX = "/api/spans";

    public static final String TOKEN_KEY = "LUMIGO_TRACER_TOKEN";
    public static final String TRACER_HOST_KEY = "LUMIGO_TRACER_HOST";
    public static final String DEBUG_KEY = "LUMIGO_DEBUG";
    public static final String REGION_KEY = "AWS_REGION";
    public static final String LUMIGO_VERBOSE = "LUMIGO_VERBOSE";
    public static final String REPORTER_TIMEOUT = "LUMIGO_REPORTER_TIMEOUT";
    public static final String LUMIGO_KILL_SWITCH = "LUMIGO_SWITCH_OFF";
    public static final String LUMIGO_MAX_ENTRY_SIZE = "LUMIGO_MAX_ENTRY_SIZE";
    public static final String LUMIGO_INSTRUMENTATION = "LUMIGO_INSTRUMENTATION";

    private static Configuration instance;
    private LumigoConfiguration inlineConf;

    @Setter private EnvUtil envUtil = new EnvUtil();

    public static void init(LumigoConfiguration lumigoConfiguration) {
        if (lumigoConfiguration == null) {
            getInstance().inlineConf = LumigoConfiguration.builder().build();
        } else {
            getInstance().inlineConf = lumigoConfiguration;
        }
        if (!getInstance().inlineConf.getLazyLoading()) {
            Logger.info("Lazy load was set as false, install agent now");
            Installer.install();
        }
    }

    private Configuration() {
        inlineConf = LumigoConfiguration.builder().build();
    }

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
            Configurator.currentConfig()
                    .writer(new ConsoleWriter())
                    .locale(Locale.US)
                    .level(instance.getLogLevel())
                    .maxStackTraceElements(-1)
                    .formatPattern(
                            "{date:yyyy-MM-dd HH:mm:ss} {level} [thread-{thread}] {class}.{method}() - {message}")
                    .activate();
        }
        return instance;
    }

    public String getLumigoToken() {
        return inlineConf.getToken() != null ? inlineConf.getToken() : envUtil.getEnv(TOKEN_KEY);
    }

    public String getLumigoEdge() {
        String url =
                inlineConf.getEdgeHost() != null
                        ? inlineConf.getEdgeHost()
                        : envUtil.getEnv(TRACER_HOST_KEY);
        if (url == null) {
            url = String.format(EDGE_DEFAULT_URL, envUtil.getEnv(REGION_KEY));
        }
        return EDGE_PREFIX + url + EDGE_SUFFIX;
    }

    public Level getLogLevel() {
        return envUtil.getBooleanEnv(DEBUG_KEY, false) ? Level.DEBUG : Level.OFF;
    }

    public String getLumigoTracerVersion() {
        return "1.0.34";
    }

    public Duration getLumigoTimeout() {
        String timeout = envUtil.getEnv(REPORTER_TIMEOUT);
        if (timeout != null) {
            return Duration.ofMillis(Long.parseLong(timeout));
        }
        return Duration.ofMillis(3000);
    }

    public int maxSpanFieldSize() {
        return envUtil.getIntegerEnv(LUMIGO_MAX_ENTRY_SIZE, 1024);
    }

    public int maxSpanFieldSizeWhenError() {
        return maxSpanFieldSize() * 10;
    }

    public boolean isAwsEnvironment() {
        return envUtil.getEnv("LAMBDA_RUNTIME_DIR") != null;
    }

    public boolean isLumigoVerboseMode() {
        return inlineConf.getVerbose() != null
                ? inlineConf.getVerbose()
                : envUtil.getBooleanEnv(LUMIGO_VERBOSE, true);
    }

    public boolean isKillingSwitchActivated() {
        return inlineConf.getKillSwitch() != null
                ? inlineConf.getKillSwitch()
                : envUtil.getBooleanEnv(LUMIGO_KILL_SWITCH, false);
    }

    public boolean isLumigoHost(String host) {
        return host.contains(getLumigoEdge().replace(EDGE_PREFIX, "").replace(EDGE_SUFFIX, ""));
    }

    public boolean isAwsHost(String host) {
        return host.endsWith("amazonaws.com");
    }

    public boolean isInstrumentationEnabled() {
        return envUtil.getBooleanEnv(LUMIGO_INSTRUMENTATION, true);
    }
}
