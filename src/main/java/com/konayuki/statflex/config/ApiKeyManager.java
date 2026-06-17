package com.konayuki.statflex.config;

public final class ApiKeyManager {
    private static String apiKey = null;

    private ApiKeyManager() {
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
