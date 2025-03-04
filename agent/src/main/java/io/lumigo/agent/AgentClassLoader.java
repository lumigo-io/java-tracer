package io.lumigo.agent;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is refered from :
 * https://github.com/gaoxingliang/tracing-research/blob/main/bootstrap/src/main/java/com/zoomphant/agent/trace/boost/StandaloneAgentClassloader.java
 */
public class AgentClassLoader extends URLClassLoader {

    private final ClassLoader additionalClassloader;

    public AgentClassLoader(URL[] urls, ClassLoader additionalClassloader) {
        super(urls, ClassLoader.getSystemClassLoader().getParent());
        this.additionalClassloader = additionalClassloader;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        if (name != null && (name.startsWith("sun.") || name.startsWith("java."))) {
            return super.loadClass(name, resolve);
        }

        ClassLoader platformClassLoader = safeGetPlatformClassLoader();
        if (platformClassLoader != null) {
            // try load from platform class loader
            try {
                loadedClass = platformClassLoader.loadClass(name);
                return loadedClass;
            } catch (Exception ignore) {
            }
        }
        if (additionalClassloader != null) {
            // try load from additional classloader
            try {
                loadedClass = additionalClassloader.loadClass(name);
                return loadedClass;
            } catch (Exception ignore) {
            }
        }
        try {
            loadedClass = this.getParent().loadClass(name);
            return loadedClass;
        } catch (Exception e) {
        }
        try {
            Class<?> aClass = findClass(name);
            if (resolve) {
                resolveClass(aClass);
            }
            return aClass;
        } catch (Exception e) {
            // ignore
        }
        return super.loadClass(name, resolve);
    }

    private ClassLoader safeGetPlatformClassLoader() {
        // get platform class loader with reflection
        try {
            return (ClassLoader) ClassLoader.class.getDeclaredMethod("getPlatformClassLoader").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }
}