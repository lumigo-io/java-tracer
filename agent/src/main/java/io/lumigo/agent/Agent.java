package io.lumigo.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Agent {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        try {
            URL[] urls;
            if ("lib".equalsIgnoreCase(agentArgs)) {
                urls = getUrls();
            } else {
                urls = new URL[] {new File("/var/task/").toURI().toURL()};
            }
            URLClassLoader newClassLoader = new URLClassLoader(urls, null);
            Thread.currentThread().setContextClassLoader(newClassLoader);
            final Class<?> loader =
                    newClassLoader.loadClass("io.lumigo.core.instrumentation.agent.Loader");
            final Method instrument = loader.getMethod("instrument", Instrumentation.class);
            instrument.invoke(null, inst);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static URL[] getUrls() {
        List<URL> jars = new LinkedList<>();
        try (Stream<Path> paths = Files.walk(Paths.get("/var/task/lib"))) {
            jars.add(new File("/var/task/").toURI().toURL());
            jars.addAll(
                    paths.map(
                                    path -> {
                                        try {
                                            return path.toFile().toURI().toURL();
                                        } catch (MalformedURLException e) {
                                            e.printStackTrace();
                                            return null;
                                        }
                                    })
                            .filter(v -> v != null)
                            .collect(Collectors.toList()));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return jars.toArray(new URL[jars.size()]);
    }
}

