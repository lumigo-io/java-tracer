package com.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.lumigo.core.SpansContainer;
import com.lumigo.core.configuration.LumigoConfiguration;
import com.lumigo.core.network.Reporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class LumigoRequestStreamHandler implements RequestStreamHandler {

    private final LumigoConfiguration lumigoConfiguration = LumigoConfiguration.getInstance();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        try {
            SpansContainer.getInstance().init(System.getenv(), context, null);
            SpansContainer.getInstance().start();
            doHandleRequest(inputStream, outputStream, context);
        } catch (Throwable e) {
            SpansContainer.getInstance().addException(e);
            throw e;
        } finally {
            SpansContainer.getInstance().end();
            Reporter.reportSpans(SpansContainer.getInstance());
        }
    }

    public abstract void doHandleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException;

}
