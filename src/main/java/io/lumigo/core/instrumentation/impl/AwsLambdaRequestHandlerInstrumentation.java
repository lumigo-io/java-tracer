package io.lumigo.core.instrumentation.impl;

import static net.bytebuddy.matcher.ElementMatchers.*;

import com.amazonaws.services.lambda.runtime.Context;
import io.lumigo.core.SpansContainer;
import io.lumigo.core.instrumentation.LumigoInstrumentationApi;
import io.lumigo.core.instrumentation.agent.Loader;
import io.lumigo.core.network.Reporter;
import io.lumigo.core.utils.EnvUtil;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.matcher.ElementMatcher;
import org.pmw.tinylog.Logger;

public class AwsLambdaRequestHandlerInstrumentation implements LumigoInstrumentationApi {
    @Override
    public ElementMatcher<TypeDescription> getTypeMatcher() {
        return hasSuperType(named("com.amazonaws.services.lambda.runtime.RequestHandler"))
                // we don't want to instrument handlers that implement our interfaces because they
                // are already instrumented
                .and(
                        not(hasSuperType(named("io.lumigo.handlers.LumigoRequestHandler")))
                                .and(
                                        not(
                                                hasSuperType(
                                                        named(
                                                                "io.lumigo.handlers.LumigoRequestStreamHandler")))));
    }

    @Override
    public AgentBuilder.Transformer.ForAdvice getTransformer() {
        return new AgentBuilder.Transformer.ForAdvice()
                .include(Loader.class.getClassLoader())
                .advice(
                        isMethod()
                                .and(isPublic())
                                .and(named("handleRequest"))
                                .and(
                                        takesArgument(
                                                1,
                                                named(
                                                        "com.amazonaws.services.lambda.runtime.Context"))),
                        AwsLambdaRequestHandlerInstrumentation.class.getName()
                                + "$HandleRequestAdvice");
    }

    @SuppressWarnings("unused")
    public static class HandleRequestAdvice {
        public static final SpansContainer spansContainer = SpansContainer.getInstance();

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void methodEnter(
                @Advice.Argument(value = 0, typing = Typing.DYNAMIC) Object input,
                @Advice.Argument(1) Context context) {
            try {
                Logger.debug("Start AwsLambdaRequestHandlerInstrumentation$HandleRequestAdvice");
                spansContainer.init(new EnvUtil().getEnv(), new Reporter(), context, input);
                spansContainer.start();
                Logger.debug("Finish sending start message and instrumentation");
            } catch (Throwable e) {
                Logger.error(e, "Failed to init span container");
            }
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void methodExit(
                @Advice.Return(readOnly = false) Object returnValue,
                @Advice.Thrown Throwable throwable) {
            try {
                if (throwable != null) {
                    spansContainer.endWithException(throwable);
                } else {
                    spansContainer.end(returnValue);
                }
            } catch (Throwable e) {
                Logger.error(e, "Failed to create end span");
            }
        }
    }
}
