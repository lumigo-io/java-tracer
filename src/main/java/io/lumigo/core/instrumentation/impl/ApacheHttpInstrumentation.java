package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.*;

import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.AgentLoad;
import java.util.*;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.pmw.tinylog.Logger;

public class ApacheHttpInstrumentation implements LumigoInstrumentationApi {

    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return hasSuperType(named("org.apache.http.client.HttpClient").and(isInterface()));
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {

        return new AgentBuilder.Transformer.ForAdvice()
                .include(AgentLoad.class.getClassLoader())
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

    @Override
    public String packageName() {
        return "org.apache.http.client";
    }

    public static class AmazonHttpClientAdvice {

        public static Set<Integer> handled = Collections.synchronizedSet(new HashSet<>());

        @Advice.OnMethodEnter
        public static void executeEnter(@Advice.Argument(0) final HttpUriRequest request) {
            try {
                if (!handled.contains(request.hashCode())) {
                    handled.add(request.hashCode());
                    Map<String, String> headers = new HashMap<>();
                    for (Header header : request.getAllHeaders()) {
                        headers.put(header.getName(), header.getValue());
                    }
                    SpansContainer.getInstance().addHttpSpan(request.getURI(), headers);
                }
            } catch (Exception e) {
                Logger.error(e, "Failed to collect http span");
            }
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void methodExit(@Advice.Return final Object result) {
            if (!handled.contains(result.hashCode())) {
                handled.add(result.hashCode());
            }
        }
    }
}
