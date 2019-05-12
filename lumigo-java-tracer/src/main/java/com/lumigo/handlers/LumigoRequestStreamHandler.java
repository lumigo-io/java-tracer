package com.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.lumigo.core.configuration.LumigoConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class LumigoRequestStreamHandler implements RequestStreamHandler {

    private final LumigoConfiguration lumigoConfiguration = LumigoConfiguration.getInstance();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        try {
            System.out.println("Hook before customer handler run");
            doHandleRequest(inputStream, outputStream, context);
        } catch (Throwable e) {
            System.out.println("Hook after customer handler have exception");
            throw e;
        } finally {
            System.out.println("Hook after customer handler run");
        }
    }

    public abstract void doHandleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException;

}
