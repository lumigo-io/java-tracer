package io.lumigo.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Agent {

    private static boolean initialized = false;

    public static String LUMIGO_JAVA_TRACER_PATH = "/opt/lumigo/lumigo-tracer.jar";

    public static void premain(String agentArgs, Instrumentation inst) {
        agentmain(agentArgs, inst);
        // TODO handle switch off
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            URL[] urls;
            if ("lib".equalsIgnoreCase(agentArgs)) {
                urls = getUrls();
            } else {
                List<URL> jars = new LinkedList<>();
                jars.add(new File("/var/task/").toURI().toURL());
                jars.add(new File(LUMIGO_JAVA_TRACER_PATH).toURI().toURL());
                urls = jars.toArray(new URL[jars.size()]);
            }
            installTracerJar(inst);
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

    private static void installTracerJar(Instrumentation inst) {
        try (JarFile jar = new JarFile(new File(new File(LUMIGO_JAVA_TRACER_PATH).toURI()))){
            inst.appendToSystemClassLoaderSearch(jar);
        } catch (Exception e) {
            System.out.println("Failed to append java tracer jar to system class loader: " + LUMIGO_JAVA_TRACER_PATH);
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
