package io.lumigo.core.instrumentation.impl;

import com.sun.tools.javac.util.List;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.core.utils.LRUCache;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.pmw.tinylog.Logger;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.Pair;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AmazonHttpClientV2Instrumentation implements LumigoInstrumentationApi {
    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
         System.out.println("successfully hooked sdk v2 matcher");
        return named("software.amazon.awssdk.core.internal.http.pipeline.stages.MakeHttpRequestStage");
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {
         System.out.println("successfully hooked sdk v2 transform");
        return new AgentBuilder.Transformer.ForAdvice()
                .include(Loader.class.getClassLoader())
                .advice(
                        isMethod()
                                .and(isPublic())
                                .and(named("execute"))
                                .and(takesArgument(0, named("software.amazon.awssdk.http.SdkHttpFullRequest"))),
                        AmazonHttpClientV2Advice.class.getName());
    }

    public static class AmazonHttpClientV2Advice {

        public static final SpansContainer spansContainer = SpansContainer.getInstance();

        public static final LRUCache<Integer, Boolean> handled = new LRUCache<>(1000);

        public static final LRUCache<Integer, Long> startTimeMap = new LRUCache<>(1000);

        @Advice.OnMethodEnter
        public static void methodEnter(@Advice.Argument(0) final SdkHttpFullRequest request, @Advice.Argument(1) final RequestExecutionContext context) {
            System.out.println("successfully hooked sdk v2 enter");
            try {
                System.out.println(request.hashCode());
                startTimeMap.put(request.hashCode(), System.currentTimeMillis());
                context.executionAttributes().putAttribute(new ExecutionAttribute<>("X-Amzn-Trace-Id"), spansContainer.getPatchedRoot());
            } catch (Exception e) {
                Logger.error(e, "Failed to send data on http requests");
            }
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void methodExit(
                @Advice.Argument(0) final SdkHttpFullRequest request, @Advice.Argument(1) final RequestExecutionContext context,
                @Advice.Return final Pair<SdkHttpFullRequest, SdkHttpFullResponse> response) {
            System.out.println("successfully hooked sdk v2 exit");
            try {
                System.out.println(request.hashCode());
                if (handled.get(request.hashCode()) == null) {
                    Logger.info("Handling request {} from host {}", request.hashCode());
                    spansContainer.addHttpSpan(startTimeMap.get(request.hashCode()), request, context, response.right());
                    handled.put(request.hashCode(), true);
                } else {
                    Logger.warn(
                            "Already handle request {} for host {}",
                            request.hashCode(),
                            request.host());
                }
            } catch (Throwable e) {
                Logger.error(e, "Failed to send data on http response");
            } finally {
                startTimeMap.remove(request.hashCode());
            }
        }
    }
}