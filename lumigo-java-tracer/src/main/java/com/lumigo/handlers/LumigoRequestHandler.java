package com.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public abstract class LumigoRequestHandler<INPUT, OUTPUT> implements RequestHandler<INPUT, OUTPUT> {

    @Override
    public OUTPUT handleRequest(INPUT input, Context context) {
        try {
            System.out.println("Hook before customer handler run");
            return doHandleRequest(input, context);
        } catch (Throwable e) {
            System.out.println("Hook after customer handler have exception");
            throw e;
        } finally {
            System.out.println("Hook after customer handler run");
        }
    }

    public abstract OUTPUT doHandleRequest(INPUT input, Context context);
}
