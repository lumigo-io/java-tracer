package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.*;

import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.core.utils.LRUCache;
import java.util.List;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

public class AmazonHttpClientV2Instrumentation implements LumigoInstrumentationApi {

    public static final String INSTRUMENTATION_PACKAGE_PREFIX =
            "software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder";

    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return named(INSTRUMENTATION_PACKAGE_PREFIX);
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer(ClassLoader classLoader) {
        return new AgentBuilder.Transformer.ForAdvice()
                .include(classLoader)
                .advice(
                        isMethod().and(named("resolveExecutionInterceptors")),
                        AmazonHttpClientV2Instrumentation.class.getName()
                                + "$AmazonHttpClientV2Advice");
    }

    @SuppressWarnings("unused")
    public static class AmazonHttpClientV2Advice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void methodExit(
                @Advice.Return final List<ExecutionInterceptor> interceptors) {
            Logger.debug("At AmazonHttpClientV2Instrumentation$AmazonHttpClientV2Advice");
            for (ExecutionInterceptor interceptor : interceptors) {
                if (interceptor instanceof TracingExecutionInterceptor) {
                    Logger.debug("Lumigo TracingExecutionInterceptor already exists, skipping...");
                    return; // list already has our interceptor, return to builder
                }
            }
            interceptors.add(new TracingExecutionInterceptor());
            Logger.debug("Added Lumigo TracingExecutionInterceptor");
        }

        public static class TracingExecutionInterceptor implements ExecutionInterceptor {
            public static final SpansContainer spansContainer = SpansContainer.getInstance();
            public static final LRUCache<Integer, Boolean> handled = new LRUCache<>(1000);
            public static final LRUCache<Integer, Long> startTimeMap = new LRUCache<>(1000);

            @Override
            public void beforeExecution(
                    final Context.BeforeExecution context,
                    final ExecutionAttributes executionAttributes) {
                try {
                    startTimeMap.put(context.request().hashCode(), System.currentTimeMillis());
                } catch (Throwable e) {
                    Logger.error(e, "Failed save trace context");
                }
            }

            @Override
            public void afterExecution(
                    final Context.AfterExecution context,
                    final ExecutionAttributes executionAttributes) {
                try {
                    if (handled.get(context.request().hashCode()) == null) {
                        Logger.info(
                                "Handling request {} from host {}", context.request().hashCode());
                        spansContainer.addHttpSpan(
                                startTimeMap.get(context.request().hashCode()),
                                context,
                                executionAttributes);
                        handled.put(context.request().hashCode(), true);
                    } else {
                        Logger.warn(
                                "Already handle request {} for host {}",
                                context.request().hashCode(),
                                context.httpRequest().host());
                    }
                } catch (Throwable e) {
                    Logger.error(e, "Failed to send data on http response");
                } finally {
                    startTimeMap.remove(context.request().hashCode());
                }
            }
        }
    }
}
