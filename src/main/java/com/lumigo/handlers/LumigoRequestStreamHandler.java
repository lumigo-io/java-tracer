package com.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.lumigo.core.SpansContainer;
import com.lumigo.core.network.Reporter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.pmw.tinylog.Logger;

public abstract class LumigoRequestStreamHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        try {
            Logger.debug("Start {} Lumigo tracer", LumigoRequestStreamHandler.class.getName());
            SpansContainer.getInstance().init(System.getenv(), context, null);
            SpansContainer.getInstance().start();
            Reporter.reportSpansAsync(SpansContainer.getInstance().getStartFunctionSpan());
            doHandleRequest(inputStream, outputStream, context);
            SpansContainer.getInstance().end();
        } catch (Throwable e) {
            Logger.debug("Customer lambda had exception {}", e.getClass().getName());
            SpansContainer.getInstance().endWithException(e);
            throw e;
        } finally {
            Reporter.reportSpans(SpansContainer.getInstance().getAllCollectedSpans());
        }
    }

    public abstract void doHandleRequest(
            InputStream inputStream, OutputStream outputStream, Context context) throws IOException;
}
