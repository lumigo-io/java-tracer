package io.lumigo.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import io.lumigo.core.utils.TimeMeasure;
import lombok.Setter;
import org.pmw.tinylog.Logger;

public abstract class LumigoRequestHandler<INPUT, OUTPUT> implements RequestHandler<INPUT, OUTPUT> {

    @Setter private EnvUtil envUtil = new EnvUtil();
    @Setter private Reporter reporter = new Reporter();

    @Override
    public OUTPUT handleRequest(INPUT input, Context context) {
        try (TimeMeasure ignored1 = new TimeMeasure("Full handler executions")) {
            Logger.debug("Start {} Lumigo tracer", LumigoRequestHandler.class.getName());
            try (TimeMeasure ignored = new TimeMeasure("Init SpansContainer")) {
                SpansContainer.getInstance().init(envUtil.getEnv(), context, input);
                SpansContainer.getInstance().start();
            } catch (Throwable e) {
                Logger.error(e, "Failed to init span container");
            }
            try (TimeMeasure ignored = new TimeMeasure("Report start span")) {
                reporter.reportSpans(SpansContainer.getInstance().getStartFunctionSpan());
            } catch (Throwable e) {
                Logger.error(e, "Failed to send start span");
            }
            OUTPUT response = doHandleRequest(input, context);
            try (TimeMeasure ignored = new TimeMeasure("Create end span")) {
                SpansContainer.getInstance().end(response);
            } catch (Throwable e) {
                Logger.error(e, "Failed to create end span");
            }
            return response;
        } catch (Throwable throwable) {
            try (TimeMeasure ignored = new TimeMeasure("Create end span")) {
                Logger.debug("Customer lambda had exception {}", throwable.getClass().getName());
                SpansContainer.getInstance().endWithException(throwable);
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to create end span");
            }
            throw throwable;
        } finally {
            try (TimeMeasure ignored = new TimeMeasure("Report all spans")) {
                reporter.reportSpans(SpansContainer.getInstance().getAllCollectedSpans());
            } catch (Throwable ex) {
                Logger.error(ex, "Failed to send all spans");
            }
        }
    }

    public abstract OUTPUT doHandleRequest(INPUT input, Context context);
}
