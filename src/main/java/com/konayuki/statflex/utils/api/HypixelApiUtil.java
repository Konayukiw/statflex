package com.konayuki.statflex.utils.api;

import com.konayuki.statflex.utils.Setting;

public final class HypixelApiUtil {
    private static String apiKey = null;

    private HypixelApiUtil() {
    }

    public static void init() {
        Setting.load();
        apiKey = Setting.get().apiKey;
    }

    public static String get() {
        return apiKey != null && !apiKey.isEmpty() ? apiKey : "N/A";
    }

    public static void set(String key) {
        apiKey = key;
        Setting.get().apiKey = key;
        Setting.save();
    }
}
