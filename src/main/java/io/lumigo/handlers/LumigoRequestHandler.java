package io.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import lombok.AccessLevel;
import lombok.Setter;
import org.pmw.tinylog.Logger;

public abstract class LumigoRequestHandler<INPUT, OUTPUT> implements RequestHandler<INPUT, OUTPUT> {

    @Setter(AccessLevel.MODULE)
    private EnvUtil envUtil = new EnvUtil();

    @Setter(AccessLevel.MODULE)
    private Reporter reporter = new Reporter();

    @Setter(AccessLevel.MODULE)
    private SpansContainer spansContainer = SpansContainer.getInstance();

    @Override
    public OUTPUT handleRequest(INPUT input, Context context) {
        if (Configuration.getInstance().isKillingSwitchActivated()) {
            return doHandleRequest(input, context);
        }
        try {
            Logger.debug("Start {} Lumigo tracer", LumigoRequestHandler.class.getName());
            try {
                spansContainer.init(envUtil.getEnv(), reporter, context, input);
                spansContainer.start();
            } catch (Throwable e) {
                Logger.error(e, "Failed to init span container");
            }
            OUTPUT response = doHandleRequest(input, context);
            try {
                spansContainer.end(response);
            } catch (Throwable e) {
                Logger.error(e, "Failed to create end span");
            }
            return response;
        } catch (Throwable throwable) {
            try {
                Logger.debug("Customer lambda had exception {}", throwable.getClass().getName());
                spansContainer.endWithException(throwable);
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
            throw throwable;
        }
    }

    public abstract OUTPUT doHandleRequest(INPUT input, Context context);
}
