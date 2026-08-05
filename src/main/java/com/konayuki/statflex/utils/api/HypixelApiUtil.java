package com.konayuki.statflex.utils.api;

import com.konayuki.statflex.utils.Setting;

public final class HypixelApiUtil {
    private static String apiKey = null;

    private HypixelApiUtil() {
    }

    public static void init() {
        Setting.load();
        apiKey = Setting.getInstance().apiKey;
    }

    public static void set(String key) {
        apiKey = key;
        Setting.getInstance().apiKey = key;
        Setting.save();
    }

    public static String get() {
        return apiKey != null && !apiKey.isEmpty() ? apiKey : "N/A";
    }
}