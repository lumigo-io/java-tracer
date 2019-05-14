package com.lumigo.core.configuration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import sun.rmi.runtime.Log;

public class LumigoConfiguration {
  private static final String EDGE_PREFIX = "https://";
  private static final String EDGE_DEFAULT_URL = "%s.lumigo-tracer-edge.golumigo.com";
  private static final String EDGE_SUFFIX = "/api/spans";

  private static final String TOKEN_KEY = "LUMIGO_TRACER_TOKEN";
  private static final String TRACER_HOST_KEY = "LUMIGO_TRACER_HOST";
  private static final String DEBUG_KEY = "LUMIGO_DEBUG";
  private static final String REGION_KEY = "AWS_REGION";
  private static LumigoConfiguration instance;

  public static synchronized LumigoConfiguration getInstance() {
    if (instance == null) {
      instance = new LumigoConfiguration();
    }
    return instance;
  }

  public String getLumigoToken() {
    return System.getenv(TOKEN_KEY);
  }

  public String getLumigoEdge() {
    String url = System.getenv(TRACER_HOST_KEY);
    if (url == null) {
      url = String.format(EDGE_DEFAULT_URL, System.getenv(REGION_KEY));
    }
    return EDGE_PREFIX + url + EDGE_SUFFIX;
  }

  public Level getLumigoLogLevel() {
    String debug = System.getenv(DEBUG_KEY);
    if (debug.toLowerCase().equals("true")) {
      return Level.DEBUG;
    }
    return Level.FATAL;
  }

  public String getLumigoTracerVersion() {
    return "1.0";
  }
}
