package com.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.lumigo.core.SpansContainer;
import com.lumigo.core.network.Reporter;
import org.pmw.tinylog.Logger;

public abstract class LumigoRequestHandler<INPUT, OUTPUT> implements RequestHandler<INPUT, OUTPUT> {

    @Override
    public OUTPUT handleRequest(INPUT input, Context context) {
        try {
            Logger.debug("Start {} Lumigo tracer", LumigoRequestHandler.class.getName());
            SpansContainer.getInstance().init(System.getenv(), context, input);
            SpansContainer.getInstance().start();
            Reporter.reportSpans(SpansContainer.getInstance().getStartFunctionSpan());
            OUTPUT response = doHandleRequest(input, context);
            SpansContainer.getInstance().end(response);
            return response;
        } catch (Throwable e) {
            Logger.debug("Customer lambda had exception {}", e.getClass().getName());
            SpansContainer.getInstance().endWithException(e);
            throw e;
        } finally {
            Reporter.reportSpans(SpansContainer.getInstance().getAllCollectedSpans());
        }
    }

    public abstract OUTPUT doHandleRequest(INPUT input, Context context);
}
