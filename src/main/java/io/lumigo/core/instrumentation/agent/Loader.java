package io.lumigo.core.instrumentation.agent;

import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;

import io.lumigo.core.instrumentation.impl.ApacheHttpInstrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.pmw.tinylog.Logger;

public class Loader {
    public static void instrument(java.lang.instrument.Instrumentation inst) {
        Logger.info("Start Instrumentation");
        ApacheHttpInstrumentation instrumentation = new ApacheHttpInstrumentation();
        AgentBuilder builder =
                new AgentBuilder.Default()
                        .disableClassFormatChanges()
                        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .ignore(
                                not(
                                        nameStartsWith("com.amazonaws.http.apache.client.impl")
                                                .and(
                                                        not(
                                                                nameStartsWith(
                                                                        "org.apache.http.impl.client")))))
                        .type(instrumentation.getTypeMatcher())
                        .transform(instrumentation.getTransformer());
        builder.installOn(inst);
        Logger.info("Finish Instrumentation");
    }
}
