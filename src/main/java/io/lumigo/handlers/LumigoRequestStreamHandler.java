package io.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.instrumentation.agent.Installer;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Setter;
import org.pmw.tinylog.Logger;

public abstract class LumigoRequestStreamHandler implements RequestStreamHandler {

    protected ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Setter(AccessLevel.MODULE)
    private EnvUtil envUtil;

    @Setter(AccessLevel.MODULE)
    private Reporter reporter;

    @Setter(AccessLevel.MODULE)
    private SpansContainer spansContainer;

    public LumigoRequestStreamHandler() {
        try {
            this.envUtil = new EnvUtil();
            this.reporter = new Reporter();
            this.spansContainer = SpansContainer.getInstance();
        } catch (RuntimeException ex) {
            Logger.error(ex, "Failed to init LumigoRequestStreamHandler");
        }
    }

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
                spansContainer.init(envUtil.getEnv(), reporter, context, null);
                Future<?> submit = executorService.submit(() -> Installer.install());
                spansContainer.start();
                submit.get();
                Logger.debug("Finish sending start message and instrumentation");
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to init span container");
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
        }
    }

    public abstract void doHandleRequest(
            InputStream inputStream, OutputStream outputStream, Context context) throws IOException;
}
