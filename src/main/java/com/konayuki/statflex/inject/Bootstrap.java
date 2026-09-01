package com.konayuki.statflex.inject;

public final class Bootstrap {
    private static volatile boolean requested;
    private static volatile boolean started;
    private Bootstrap() {
    }
    public static void requestStart() {
        requested = true;
    }

    public static void tick() {
        if (!requested || started) {
            return;
        }
        started = true;
        try {
            Class<?> launcher = Class.forName("com.konayuki.statflex.launch.Bootstrap",
                    true, Bootstrap.class.getClassLoader());
            launcher.getMethod("fromInjection").invoke(null);
        } catch (Throwable t) {
            Log.throwable("Client failed to start", t);
        }
    }
    public static boolean isStarted() {
        return started;
    }
    private static void log(String message) {
        Log.line(message);
    }
}
