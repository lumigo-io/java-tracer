package io.lumigo.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class Agent {

    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            String className = "io.lumigo.core.instrumentation.agent.Loader";
            final Class<?> loader = Class.forName(className);
            final Method instrument = loader.getMethod("instrument", Instrumentation.class);
            instrument.invoke(null, inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
