package com.konayuki.statflex;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;

public final class JavaAgent {
    private JavaAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        launch();
    }

    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        launch();
    }

    public static void premain(String agentArgs) {
        launch();
    }

    public static void agentmain(String agentArgs) {
        launch();
    }

    private static void launch() {
        try {
            JarLauncher.launch(getOwnJarUrl(), Thread.currentThread().getContextClassLoader());
            System.out.println("[S] Agent entrypoint completed.");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static URL getOwnJarUrl() {
        CodeSource codeSource = JavaAgent.class.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            throw new IllegalStateException("Unable to resolve statflex jar location.");
        }
        return codeSource.getLocation();
    }
}
