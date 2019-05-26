package io.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.AccessLevel;
import lombok.Setter;
import org.pmw.tinylog.Logger;

public abstract class LumigoRequestStreamHandler implements RequestStreamHandler {

    @Setter(AccessLevel.MODULE)
    private EnvUtil envUtil = new EnvUtil();

    @Setter(AccessLevel.MODULE)
    private Reporter reporter = new Reporter();

    @Setter(AccessLevel.MODULE)
    private SpansContainer spansContainer = SpansContainer.getInstance();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        if (Configuration.getInstance().isKillingSwitchActivated()) {
            doHandleRequest(inputStream, outputStream, context);
            return;
        }
        try {
            Logger.debug("Start {} Lumigo tracer", LumigoRequestStreamHandler.class.getName());
            try {
                spansContainer.init(envUtil.getEnv(), context, null);
                spansContainer.start();
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to init span container");
            }
            try {
                reporter.reportSpans(spansContainer.getStartFunctionSpan());
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
            doHandleRequest(inputStream, outputStream, context);
            try {
                spansContainer.end();
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
        } catch (Throwable throwable) {
            Logger.debug("Customer lambda had exception {}", throwable.getClass().getName());
            try {
                spansContainer.endWithException(throwable);
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
            throw throwable;
        } finally {
            try {
                reporter.reportSpans(spansContainer.getAllCollectedSpans());
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to send all spans");
            }
        }
    }

    public abstract void doHandleRequest(
            InputStream inputStream, OutputStream outputStream, Context context) throws IOException;
}
