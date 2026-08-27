package com.konayuki.statflex.utils.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.Connection;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Setting;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HypixelApiUtil {
    private static String apiKey = null;

    private static volatile Boolean keyValidated = null;

    private HypixelApiUtil() {
    }

    public static void init() {
        Setting.load();
        apiKey = Setting.get().apiKey;
        validateAsync();
    }

    public static String get() {
        return apiKey != null && !apiKey.isEmpty() ? apiKey : "N/A";
    }

    public static void set(String key) {
        apiKey = key;
        Setting.get().apiKey = key;
        Setting.save();
        validateAsync();
    }

    public static boolean isKeylessMode() {
        Boolean validated = keyValidated;
        if (validated != null) {
            return !validated;
        }
        return apiKey == null || apiKey.isEmpty();
    }

    public static void markInvalid() {
        if (!Boolean.FALSE.equals(keyValidated)) {
        }
        keyValidated = Boolean.FALSE;
    }

    public static void validateAsync() {
        Thread checker = new Thread(() -> {
            try {
                keyValidated = validate(apiKey);
            } catch (Throwable throwable) {
                Debug.error("API check failed: " + throwable.getClass().getSimpleName());
                keyValidated = Boolean.FALSE;
            }
        }, "statflex-api-check");
        checker.setDaemon(true);
        checker.start();
    }

    private static boolean validate(String candidate) throws Exception {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.hypixel.net/v2/key").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("API-Key", candidate.trim());
        conn.setRequestProperty("User-Agent", "statflex");
        Connection.trust(conn);

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            return false;
        }

        InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
        JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
        boolean ok = response.has("success") && response.get("success").isJsonPrimitive()
                && response.get("success").getAsBoolean();
        return ok;
    }
}

