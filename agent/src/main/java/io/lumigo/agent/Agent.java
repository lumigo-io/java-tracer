package io.lumigo.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class Agent {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        try {
            URL[] news = {new File("/var/task/").toURI().toURL()};
            URLClassLoader newClassLoader = new URLClassLoader(news, null);
            Thread.currentThread().setContextClassLoader(newClassLoader);
            final Class<?> loader =
                    newClassLoader.loadClass("io.lumigo.core.instrumentation.agent.Loader");
            final Method instrument = loader.getMethod("instrument", Instrumentation.class);
            instrument.invoke(null, inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
