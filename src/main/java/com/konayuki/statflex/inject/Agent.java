package com.konayuki.statflex.inject;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Agent {
    private static volatile boolean installed;
    private Agent() {
    }
    public static void premain(String args, Instrumentation instrumentation) {
        install(instrumentation, false);
    }
    public static void agentmain(String args, Instrumentation instrumentation) {
        install(instrumentation, true);
    }
    private static synchronized void install(Instrumentation instrumentation, boolean live) {
        if (installed) {
            return;
        }
        installed = true;
        if ("true".equals(System.getProperty("statflex.initialized"))) {
            return;
        }

        try {
            java.net.URL self = Agent.class.getProtectionDomain().getCodeSource().getLocation();
            java.io.File jar = new java.io.File(self.toURI());
            if (jar.isFile()) {
                instrumentation.appendToSystemClassLoaderSearch(new java.util.jar.JarFile(jar));
            }
        } catch (Throwable t) {
        }

        Hook.register();
        HookTransformer transformer = new HookTransformer(instrumentation.getAllLoadedClasses());
        ClassLoader gameLoader = installIntoGameLoader(transformer.targets());
        if (gameLoader == null) {
            for (String line : transformer.drain()) {
                log("  " + line);
            }
            return;
        }

        instrumentation.addTransformer(transformer, true);
        if (live) {
            List<Class<?>> targets = transformer.targets();
            for (Class<?> target : targets) {
                if (!instrumentation.isModifiableClass(target)) {
                    continue;
                }
                try {
                    instrumentation.retransformClasses(target);
                } catch (UnmodifiableClassException e) {
                    log("Retransform refused " + target.getName() + ": " + e);
                } catch (Throwable t) {
                    log("Retransform failed " + target.getName() + ": " + t);
                }
            }
        }
        for (String line : transformer.drain()) {
            log("  " + line);
        }
        List<String> unresolved = new ArrayList<String>(MappingBridge.failures());
        if (!unresolved.isEmpty()) {
            for (String failure : unresolved) {
                log("  " + failure);
            }
        }
        if (live) {
            startClient(gameLoader);
        }
    }
    private static ClassLoader installIntoGameLoader(List<Class<?>> targets) {
        File jar;
        try {
            jar = new File(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Throwable t) {
            log("Cannot locate own jar: " + t);
            return null;
        }

        Set<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        for (Class<?> target : targets) {
            ClassLoader cl = target.getClassLoader();
            if (cl != null) {
                loaders.add(cl);
            }
        }
        ClassLoader chosen = null;
        for (ClassLoader loader : loaders) {
            if (equip(loader, jar)) {
                chosen = loader;
            }
        }
        return chosen;
    }
    private static boolean equip(ClassLoader loader, File jar) {
        try {
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(loader, jar.toURI().toURL());
        } catch (Throwable t) {
            log("Cannot add the jar to " + name(loader) + ": " + t);
            return false;
        }
        forgetFailedLookups(loader);
        try {
            Class<?> callbacks = Class.forName("com.konayuki.statflex.inject.Callback", false, loader);
            log("Callbacks resolve from " + name(loader)
                    + ", defined by " + name(callbacks.getClassLoader()));
            return true;
        } catch (Throwable t) {
            log("Callbacks unreachable from " + name(loader) + ": " + t);
            return false;
        }
    }

    private static void forgetFailedLookups(ClassLoader loader) {
        for (Class<?> c = loader.getClass(); c != null; c = c.getSuperclass()) {
            Field field;
            try {
                field = c.getDeclaredField("invalidClasses");
            } catch (NoSuchFieldException absent) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(loader);
                if (!(value instanceof Collection)) {
                    return;
                }
                int dropped = 0;
                for (Iterator<?> it = ((Collection<?>) value).iterator(); it.hasNext(); ) {
                    Object entry = it.next();
                    if (entry instanceof String && ((String) entry).startsWith("com.konayuki.statflex.")) {
                        it.remove();
                        dropped++;
                    }
                }
                if (dropped > 0) {
                    log("Dropped " + dropped + " cached lookup failures from " + name(loader));
                }
            } catch (Throwable t) {
                log("Could not clear negative caches on " + name(loader) + ": " + t);
            }
            return;
        }
    }
    private static String name(ClassLoader loader) {
        return loader == null ? "bootstrap" : loader.getClass().getSimpleName();
    }
    private static void startClient(ClassLoader gameLoader) {
        try {
            Class<?> bootstrap = Class.forName("com.konayuki.statflex.inject.Bootstrap", true, gameLoader);
            bootstrap.getMethod("requestStart").invoke(null);
        } catch (Throwable t) {
            log("Could not queue client construction: " + t);
        }
    }
    static void log(String message) {
        Log.line(message);
    }
}
