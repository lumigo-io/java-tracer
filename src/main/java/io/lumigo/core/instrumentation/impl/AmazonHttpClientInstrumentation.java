package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.amazonaws.Request;
import com.amazonaws.Response;
import io.lumigo.core.SpansCreator;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.LRUCache;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
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

        public static final Reporter reporter = new Reporter();

        public static final SpansCreator spansCreator = new SpansCreator();

        public static final LRUCache<Integer, Boolean> handled = new LRUCache<>(1000);

        public static final LRUCache<Integer, Long> startTimeMap = new LRUCache<>(1000);

        @Advice.OnMethodEnter
        public static void methodEnter(@Advice.Argument(0) final Request<?> request) {
            try {
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
                    String invocationId = UUID.randomUUID().toString();
                    List<Object> spans = new LinkedList<>();
                    spans.add(
                            spansCreator.createHttpSpan(
                                    startTimeMap.get(request.hashCode()),
                                    invocationId,
                                    request,
                                    response));
                    spans.add(
                            spansCreator.createContainerSpan(
                                    startTimeMap.get(request.hashCode()),
                                    invocationId,
                                    System.getenv()));
                    reporter.reportSpans(spans);
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
