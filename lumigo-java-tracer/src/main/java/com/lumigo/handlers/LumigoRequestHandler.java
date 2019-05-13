package com.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.lumigo.core.SpansContainer;
import com.lumigo.core.network.Reporter;

public abstract class LumigoRequestHandler<INPUT, OUTPUT> implements RequestHandler<INPUT, OUTPUT> {

    @Override
    public OUTPUT handleRequest(INPUT input, Context context) {
        try {
            SpansContainer.getInstance().init(System.getenv(), context, input);
            SpansContainer.getInstance().start();
            OUTPUT response = doHandleRequest(input, context);
            return response;
        } catch (Throwable e) {
            SpansContainer.getInstance().addException(e);
            throw e;
        } finally {
            SpansContainer.getInstance().end();
            Reporter.reportSpans(SpansContainer.getInstance());
        }
    }

    public abstract OUTPUT doHandleRequest(INPUT input, Context context);
}
