package io.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.instrumentation.agent.Installer;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.Setter;
import org.pmw.tinylog.Logger;

public class LumigoRequestExecutor {

    protected ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static LumigoRequestExecutor instance = new LumigoRequestExecutor();

    @Setter(AccessLevel.MODULE)
    private EnvUtil envUtil;

    @Setter(AccessLevel.MODULE)
    private Reporter reporter;

    @Setter(AccessLevel.MODULE)
    private SpansContainer spansContainer;

    public LumigoRequestExecutor() {
        try {
            this.envUtil = new EnvUtil();
            this.reporter = new Reporter();
            this.spansContainer = SpansContainer.getInstance();
        } catch (RuntimeException ex) {
            Logger.error(ex, "Failed to init LumigoRequestHandler");
        }
    }

    public static LumigoRequestExecutor getInstance() {
        return instance;
    }

    public static void init() {
        instance = new LumigoRequestExecutor();
    }

    public static <OUTPUT, INPUT> OUTPUT execute(
            INPUT input, Context context, Supplier<OUTPUT> handler) {
        return getInstance().executeInternal(input, context, handler);
    }

    private <OUTPUT, INPUT> OUTPUT executeInternal(
            INPUT input, Context context, Supplier<OUTPUT> handler) {
        if (Configuration.getInstance().isKillingSwitchActivated()) {
            return handler.get();
        }
        try {
            Logger.debug("Start {} Lumigo tracer", LumigoRequestExecutor.class.getName());
            try {
                spansContainer.init(envUtil.getEnv(), reporter, context, input);
                Future<?> submit = executorService.submit(() -> Installer.install());
                spansContainer.start();
                submit.get();
                Logger.debug("Finish sending start message and instrumentation");
            } catch (Throwable e) {
                Logger.error(e, "Failed to init span container");
            }
            OUTPUT response = handler.get();
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
}
