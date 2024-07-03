package io.lumigo.core.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public interface LumigoInstrumentationApi {

    ElementMatcher<TypeDescription> getTypeMatcher();

    AgentBuilder.Transformer.ForAdvice getTransformer(ClassLoader classLoader);
}
