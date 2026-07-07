package com.konayuki.statflex.utils;

public final class Debug {
    private Debug() {
    }

    public static void log(String message) {
        System.out.println("[S] " + message);
    }

    public static void log(String format, Object... args) {
        System.out.printf("[S] " + format + "%n", args);
    }

    public static void error(String message) {
        System.err.println("[S] " + message);
    }

    public static void error(String format, Object... args) {
        System.err.printf("[S] " + format + "%n", args);
    }
}
