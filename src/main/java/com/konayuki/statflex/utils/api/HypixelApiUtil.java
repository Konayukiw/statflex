package com.konayuki.statflex.utils.api;

import com.konayuki.statflex.utils.Settings;

public final class HypixelApiUtil {
    private static String apiKey = null;

    private HypixelApiUtil() {
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