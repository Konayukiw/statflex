package com.konayuki.statflex.utils;

public final class HypixelApiUtils {
    private static String apiKey = null;

    private HypixelApiUtils() {
    }

    public static void init() {
        Settings.load();
        apiKey = Settings.getInstance().apiKey;
    }

    public static void setApiKey(String key) {
        apiKey = key;
        Settings.getInstance().apiKey = key;
        Settings.save();
    }

    public static String getApiKey() {
        return apiKey != null && !apiKey.isEmpty() ? apiKey : "N/A";
    }
}
