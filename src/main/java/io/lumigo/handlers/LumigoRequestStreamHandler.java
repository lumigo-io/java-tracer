package io.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.Setter;
import org.pmw.tinylog.Logger;

public abstract class LumigoRequestStreamHandler implements RequestStreamHandler {

    @Setter private EnvUtil envUtil = new EnvUtil();
    @Setter private Reporter reporter = new Reporter();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        try {
            Logger.debug("Start {} Lumigo tracer", LumigoRequestStreamHandler.class.getName());
            try {
                SpansContainer.getInstance().init(envUtil.getEnv(), context, null);
                SpansContainer.getInstance().start();
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to init span container");
            }
            try {
                reporter.reportSpans(SpansContainer.getInstance().getStartFunctionSpan());
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
            doHandleRequest(inputStream, outputStream, context);
            try {
                SpansContainer.getInstance().end();
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
        } catch (Throwable throwable) {
            Logger.debug("Customer lambda had exception {}", throwable.getClass().getName());
            try {
                SpansContainer.getInstance().endWithException(throwable);
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
            throw throwable;
        } finally {
            try {
                reporter.reportSpans(SpansContainer.getInstance().getAllCollectedSpans());
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to send all spans");
            }
        }
    }

    public abstract void doHandleRequest(
            InputStream inputStream, OutputStream outputStream, Context context) throws IOException;
}
