package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.core.utils.LRUCache;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.pmw.tinylog.Logger;

public class AmazonHttpClientInstrumentation implements LumigoInstrumentationApi {

    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return named("com.amazonaws.http.AmazonHttpClient");
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {

        return new AgentBuilder.Transformer.ForAdvice()
                .include(Loader.class.getClassLoader())
                .advice(isMethod().and(named("execute")), AmazonHttpClientAdvice.class.getName());
    }

    public static class AmazonHttpClientAdvice {

        public static final SpansContainer spansContainer = SpansContainer.getInstance();

        public static final LRUCache<Integer, Boolean> handled = new LRUCache<>(1000);

        public static final LRUCache<Integer, Long> startTimeMap = new LRUCache<>(1000);

        @Advice.OnMethodEnter
        public static void methodEnter(@Advice.Argument(0) final Request<?> request) {
            try {
                String patchedRoot = spansContainer.getPatchedRoot();
                request.getHeaders().put("X-Amzn-Trace-Id", patchedRoot);
                startTimeMap.put(request.hashCode(), System.currentTimeMillis());
            } catch (Exception e) {
                Logger.error(e, "Failed to send data on http requests");
            }
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void methodExit(
                @Advice.Argument(0) final Request<?> request,
                @Advice.Return final Response<?> response) {
            try {
                if (handled.get(request.hashCode()) == null) {
                    Logger.info(
                            "Handling request {} from host {}",
                            request.hashCode(),
                            request.getEndpoint());
                    spansContainer.addHttpSpan(
                            startTimeMap.get(request.hashCode()), request, response);
                    handled.put(request.hashCode(), true);
                } else {
                    Logger.warn(
                            "Already handle request {} for host {}",
                            request.hashCode(),
                            request.getEndpoint().getHost());
                }
            } catch (Throwable e) {
                Logger.error(e, "Failed to send data on http response");
            } finally {
                startTimeMap.remove(request.hashCode());
            }
        }
    }
}
