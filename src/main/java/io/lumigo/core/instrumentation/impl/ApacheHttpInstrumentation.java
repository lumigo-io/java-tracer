package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.*;

import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import java.util.*;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.http.HttpResponse;
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

        public static final Set<Integer> handled = Collections.synchronizedSet(new HashSet<>());

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void methodExit(
                @Advice.Argument(0) HttpUriRequest request, @Advice.Return final Object result) {
            if (!handled.contains(result.hashCode())) {
                Logger.warn("Handle http request response");
                if (result instanceof HttpResponse)
                    Logger.info(
                            "Got new response "
                                    + ((HttpResponse) result).getStatusLine().getStatusCode());
                handled.add(result.hashCode());
            }else {
                Logger.warn("Handle request again");
            }
        }
    }
}
