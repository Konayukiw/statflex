package com.konayuki.statflex.utils.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.api.ProfileUtil.PlayerInfo;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class HypixelApi {
    public static final String INVALID_API = "INVALID_API";
    public static final String PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND";

    private HypixelApi() {
    }

    public static FetchResult fetchPlayer(String inputName) {
        String apiKey = HypixelApiUtil.getApiKey();
        if (apiKey.equals("N/A")) {
            return FetchResult.failure(INVALID_API, null, null);
        }

        PlayerInfo info = ProfileUtil.getPlayerInfo(inputName);
        if (info == null) {
            return FetchResult.failure(PLAYER_NOT_FOUND, null, inputName);
        }

        try {
            String url = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + info.uuid;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            int status = connection.getResponseCode();
            InputStreamReader reader = status >= 200 && status < 300
                    ? new InputStreamReader(connection.getInputStream())
                    : new InputStreamReader(connection.getErrorStream());

            JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
            if (!response.has("success") || !response.get("success").getAsBoolean()) {
                String cause = response.has("cause") && !response.get("cause").isJsonNull()
                        ? response.get("cause").getAsString()
                        : "Unknown error";
                String lower = cause.toLowerCase();
                if (lower.contains("invalid") || lower.contains("api key")) {
                    return FetchResult.failure(INVALID_API, null, info.name);
                }
                return FetchResult.failure(cause, null, info.name);
            }

            JsonElement playerElement = response.get("player");
            if (playerElement == null || playerElement.isJsonNull()) {
                return FetchResult.failure(PLAYER_NOT_FOUND, null, info.name);
            }

            return FetchResult.success(playerElement.getAsJsonObject(), info.name);
        } catch (Exception e) {
            return FetchResult.failure(e.getClass().getSimpleName(), e, info.name);
        }
    }

    public static final class FetchResult {
        public final boolean success;
        public final JsonObject player;
        public final String properName;
        public final String errorCode;
        public final Exception exception;

        private FetchResult(boolean success, JsonObject player, String properName, String errorCode, Exception exception) {
            this.success = success;
            this.player = player;
            this.properName = properName;
            this.errorCode = errorCode;
            this.exception = exception;
        }

        public static FetchResult success(JsonObject player, String properName) {
            return new FetchResult(true, player, properName, null, null);
        }

        public static FetchResult failure(String errorCode, Exception exception, String properName) {
            return new FetchResult(false, null, properName, errorCode, exception);
        }
    }
}