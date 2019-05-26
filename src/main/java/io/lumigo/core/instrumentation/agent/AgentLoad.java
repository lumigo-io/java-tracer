package io.lumigo.core.instrumentation.agent;

import static net.bytebuddy.matcher.ElementMatchers.*;

import com.ea.agentloader.AgentLoader;
import io.lumigo.core.instrumentation.impl.ApacheHttpInstrumentation;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.agent.builder.AgentBuilder;

public class AgentLoad {

    private static boolean firstDeploy = true;

    public static void agentmain(String agentArgs, Instrumentation inst) {
        ApacheHttpInstrumentation instrumentation = new ApacheHttpInstrumentation();
        AgentBuilder builder =
                new AgentBuilder.Default()
                        .disableClassFormatChanges()
                        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                        .ignore(isInterface())
                        .type(instrumentation.getTypeMatcher(), any())
                        .transform(instrumentation.getTransformer());
        builder.installOn(inst);
    }

    public static void load() {
        if (firstDeploy) {
            AgentLoader.loadAgentClass(AgentLoad.class.getName(), null);
            firstDeploy = false;
        }
    }
}
