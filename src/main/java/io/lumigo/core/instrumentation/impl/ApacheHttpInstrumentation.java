package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.*;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.configuration.Configuration;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.pmw.tinylog.Logger;

public class ApacheHttpInstrumentation implements LumigoInstrumentationApi {

    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return hasSuperType(named("org.apache.http.client.HttpClient").and(isInterface()));
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {

        return new AgentBuilder.Transformer.ForAdvice()
                .include(Loader.class.getClassLoader())
                .advice(
                        isMethod()
                                .and(named("execute"))
                                .and(
                                        not(isAbstract())
                                                .and(
                                                        takesArgument(
                                                                0,
                                                                named(
                                                                        "org.apache.http.client.methods.HttpUriRequest")))),
                        AmazonHttpClientAdvice.class.getName());
    }

    public static class AmazonHttpClientAdvice {

        public static final Cache<Integer, Boolean> handled =
                CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(1000).build();
        public static final Cache<Integer, Long> startTimeMap =
                CacheBuilder.newBuilder().concurrencyLevel(1).maximumSize(1000).build();

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void methodExit(
                @Advice.Argument(0) HttpUriRequest request, @Advice.Return final Object result) {
            try {
                if (Configuration.getInstance().isLumigoHost(request.getURI().getHost())) {
                    Logger.info("Skip, internal lumigo reporter");
                    return;
                }
                if (handled.getIfPresent(request.hashCode()) == null) {
                    Logger.info(
                            "Handling request {} from host {}",
                            request.hashCode(),
                            request.getURI().getHost());
                    if (result instanceof HttpResponse) {
                        SpansContainer.getInstance()
                                .addHttpSpan(
                                        startTimeMap.getIfPresent(request.hashCode()),
                                        request,
                                        (HttpResponse) result);
                        handled.put(request.hashCode(), true);
                    }
                } else {
                    Logger.warn(
                            "Already handle request {} for host {}",
                            request.hashCode(),
                            request.getURI().getHost());
                }
            } catch (Throwable e) {
                Logger.error(e, "Failed to send data on http requests");
            }
        }
    }
}
