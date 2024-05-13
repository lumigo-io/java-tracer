package io.lumigo.core.instrumentation.impl;

import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.core.utils.LRUCache;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AmazonHttpClientV2Instrumentation implements LumigoInstrumentationApi {
    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        System.out.println("successfully hooked sdk v2 matcher v2");
        return named("software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder");
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {
        System.out.println("successfully hooked sdk v2 transform v2");
        return new AgentBuilder.Transformer.ForAdvice()
                .include(Loader.class.getClassLoader())
                .advice(
                        isMethod().and(named("resolveExecutionInterceptors")),
                        AmazonHttpClientV2Advice.class.getName()
                );
    }

    public static class AmazonHttpClientV2Advice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void methodExit(@Advice.Return final List<ExecutionInterceptor> interceptors) {
            System.out.println("successfully added Lumigo TracingExecutionInterceptor");
            for (ExecutionInterceptor interceptor : interceptors) {
                if (interceptor instanceof TracingExecutionInterceptor) {
                    return; // list already has our interceptor, return to builder
                }
            }
            interceptors.add(new TracingExecutionInterceptor());
        }

        public static class TracingExecutionInterceptor implements ExecutionInterceptor {
            public static final SpansContainer spansContainer = SpansContainer.getInstance();
            public static final LRUCache<Integer, Boolean> handled = new LRUCache<>(1000);
            public static final LRUCache<Integer, Long> startTimeMap = new LRUCache<>(1000);

            @Override
            public void beforeExecution(
                    final Context.BeforeExecution context, final ExecutionAttributes executionAttributes) {
                System.out.println("Enter beforeExecution");
                startTimeMap.put(context.request().hashCode(), System.currentTimeMillis());
                System.out.println("added request: " + context.request().hashCode());
            }

            @Override
            public SdkHttpRequest modifyHttpRequest(
                    Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
                try {
                    System.out.println("Enter modifyHttpRequest");
                    SdkHttpRequest.Builder requestBuilder = context.httpRequest().toBuilder();
                    requestBuilder.appendHeader("X-Amzn-Trace-Id", spansContainer.getPatchedRoot());
                    SdkHttpRequest request = requestBuilder.build();
                    System.out.println(request.hashCode());
                    return request;
                } catch (Throwable e) {
                    Logger.debug("Unable to inject trace header", e);
                }
                return context.httpRequest();
            }

            @Override
            public void afterExecution(
                    final Context.AfterExecution context, final ExecutionAttributes executionAttributes) {
                try {
                    System.out.println("Enter afterExecution");
                    System.out.println("request: " +  context.request().hashCode());
                    if (handled.get(context.request().hashCode()) == null) {
                        Logger.info("Handling request {} from host {}", context.request().hashCode());
                        spansContainer.addHttpSpan(startTimeMap.get(context.request().hashCode()), context, executionAttributes);
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