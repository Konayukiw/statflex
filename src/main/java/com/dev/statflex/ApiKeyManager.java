package com.dev.statflex;

public class ApiKeyManager {
    private static String apiKey = null;

    public static void init() {
        SettingsManager.load();
        apiKey = SettingsManager.getInstance().apiKey;
    }

    public static void setApiKey(String key) {
        apiKey = key;
        SettingsManager.getInstance().apiKey = key;
        SettingsManager.save();
    }

    public static String getApiKey() {
        return apiKey != null && !apiKey.isEmpty() ? apiKey : "N/A";
    }
}
