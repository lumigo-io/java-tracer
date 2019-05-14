package com.lumigo.core.configuration;

public class LumigoConfiguration {
    public static final String EDGE_DEFAULT_URL = "https://5xbf178tgj.execute-api.eu-west-1.amazonaws.com/api/spans";
    private static final String LUMIGO_TOKEN = "t_347f4e0823dbe4d78154";
    private static LumigoConfiguration instance;


    private final String lumigoToken;

    private LumigoConfiguration(String lumigoToken) {
        this.lumigoToken = lumigoToken;
    }

    public synchronized static LumigoConfiguration getInstance() {
        if (instance == null) {
            instance = new LumigoConfiguration(System.getenv(LUMIGO_TOKEN));
        }
        return instance;
    }

    public String getLumigoToken() {
        return this.lumigoToken;
    }

    public String getLumigoTracerVersion() {
        return "1.0";
    }
}
