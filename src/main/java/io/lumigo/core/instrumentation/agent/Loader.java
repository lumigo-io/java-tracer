package io.lumigo.core.instrumentation.agent;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

import io.lumigo.core.instrumentation.impl.AmazonHttpClientInstrumentation;
import io.lumigo.core.instrumentation.impl.ApacheHttpInstrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.pmw.tinylog.Logger;

public class Loader {
    public static void instrument(java.lang.instrument.Instrumentation inst) {
        Logger.debug("Start Instrumentation");
        ApacheHttpInstrumentation apacheHttpInstrumentation = new ApacheHttpInstrumentation();
        AmazonHttpClientInstrumentation amazonHttpClientInstrumentation =
                new AmazonHttpClientInstrumentation();
        AgentBuilder builder =
                new AgentBuilder.Default()
                        .disableClassFormatChanges()
                        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .ignore(
                                not(nameStartsWith("com.amazonaws.http.AmazonHttpClient"))
                                        .and(not(nameStartsWith("org.apache.http.impl.client"))))
                        .type(apacheHttpInstrumentation.getTypeMatcher())
                        .transform(apacheHttpInstrumentation.getTransformer())
                        .type(amazonHttpClientInstrumentation.getTypeMatcher())
                        .transform(amazonHttpClientInstrumentation.getTransformer());

        builder.installOn(inst);
        Logger.debug("Finish Instrumentation");
    }
}
