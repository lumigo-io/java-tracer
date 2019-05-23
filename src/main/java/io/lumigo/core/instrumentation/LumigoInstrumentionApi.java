package io.lumigo.core.instrumentation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Map;

public interface LumigoInstrumentionApi {

    ElementMatcher<TypeDescription> getTypeMatcher();
    Map<ElementMatcher, String> getTransformers();

}
