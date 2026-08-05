package com.konayuki.statflex.launch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Set;

final class JarLauncher {
    private static final String MAIN_CLASS = "com.konayuki.statflex.statflex";
    private static final String INIT_METHOD = "init";

    private JarLauncher() {
    }

    static void launch(URL jarUrl, ClassLoader preferredLoader) throws Throwable {
        ClassLoader targetLoader = findMinecraftClassLoader(preferredLoader);
        Throwable targetFailure = null;

        if (targetLoader != null) {
            try {
                addUrl(targetLoader, jarUrl);
                invokeInit(targetLoader);
                return;
            } catch (Throwable throwable) {
                targetFailure = throwable;
            }
        }

        ClassLoader parent = targetLoader != null ? targetLoader : preferredLoader;
        URLClassLoader childLoader = new URLClassLoader(new URL[] {jarUrl}, parent);
        try {
            invokeInit(childLoader);
        } catch (Throwable throwable) {
            if (targetFailure != null && throwable != targetFailure) {
                throwable.addSuppressed(targetFailure);
            }
            throw throwable;
        }
    }

    private static ClassLoader findMinecraftClassLoader(ClassLoader preferredLoader) {
        Set<ClassLoader> candidates = new LinkedHashSet<ClassLoader>();
        addCandidate(candidates, preferredLoader);
        addCandidate(candidates, Thread.currentThread().getContextClassLoader());
        addCandidate(candidates, JarLauncher.class.getClassLoader());
        addCandidate(candidates, ClassLoader.getSystemClassLoader());

        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            addCandidate(candidates, thread.getContextClassLoader());
        }

        for (ClassLoader candidate : candidates) {
            if (canLoad(candidate, "net.minecraft.client.Minecraft")
                    && canLoad(candidate, "net.minecraftforge.common.MinecraftForge")) {
                return candidate;
            }
        }

        return null;
    }

    private static void addCandidate(Set<ClassLoader> candidates, ClassLoader loader) {
        for (ClassLoader current = loader; current != null; current = current.getParent()) {
            candidates.add(current);
        }
    }

    private static boolean canLoad(ClassLoader loader, String className) {
        if (loader == null) {
            return false;
        }

        try {
            Class.forName(className, false, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void addUrl(ClassLoader loader, URL jarUrl) throws Exception {
        if (!(loader instanceof URLClassLoader)) {
            return;
        }

        URLClassLoader urlClassLoader = (URLClassLoader) loader;
        for (URL url : urlClassLoader.getURLs()) {
            if (url.sameFile(jarUrl)) {
                return;
            }
        }

        Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        addUrl.setAccessible(true);
        addUrl.invoke(loader, jarUrl);
    }

    private static void invokeInit(ClassLoader loader) throws Throwable {
        try {
            Class<?> mainClass = Class.forName(MAIN_CLASS, true, loader);
            Method init = mainClass.getDeclaredMethod(INIT_METHOD);
            init.setAccessible(true);
            init.invoke(null);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}