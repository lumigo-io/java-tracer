package io.lumigo.core.configuration;

import io.lumigo.core.utils.EnvUtil;
import io.lumigo.handlers.LumigoConfiguration;
import java.time.Duration;
import java.util.Locale;
import lombok.Setter;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
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

    private static Configuration instance;
    private LumigoConfiguration inlineConf;
    @Setter private EnvUtil envUtil = new EnvUtil();

    public static void init(LumigoConfiguration lumigoConfiguration) {
        if (lumigoConfiguration == null) {
            getInstance().inlineConf = LumigoConfiguration.builder().build();
        } else {
            getInstance().inlineConf = lumigoConfiguration;
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
        String debug = envUtil.getEnv(DEBUG_KEY);
        if ("true".equalsIgnoreCase(debug)) {
            return Level.DEBUG;
        }
        return Level.OFF;
    }

    public String getLumigoTracerVersion() {
        return "1.0";
    }

    public Duration getLumigoTimeout() {
        String timeout = envUtil.getEnv(REPORTER_TIMEOUT);
        if (timeout != null) {
            return Duration.ofMillis(Long.parseLong(timeout));
        }
        return Duration.ofMillis(3000);
    }

    public int maxSpanFieldSize() {
        return 1024;
    }

    public boolean isAwsEnvironment() {
        return envUtil.getEnv("LAMBDA_RUNTIME_DIR") != null;
    }

    public boolean isLumigoVerboseMode() {
        String verbos =
                inlineConf.getVerbose() != null
                        ? inlineConf.getVerbose().toString()
                        : envUtil.getEnv(LUMIGO_VERBOSE);
        if ("false".equalsIgnoreCase(verbos)) {
            return false;
        }
        return true;
    }
}
