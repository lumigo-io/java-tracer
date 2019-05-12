package com.lumigo.core.configuration;

public class LumigoConfiguration {
    private static final String LUMIGO_TOKEN = "LUMIGO_TOKEN";
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
}
