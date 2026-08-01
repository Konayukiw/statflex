package com.konayuki.statflex.utils.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.api.Profile.PlayerInfo;
import com.konayuki.statflex.utils.chat.Chat;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class HypixelApi {
    public static final String INVALID_API = "INVALID_API";
    public static final String PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND";

    private HypixelApi() {
    }

    public static result Fetch(String inputName) {
        if (inputName == null || inputName.isEmpty()) {
            return result.failure(PLAYER_NOT_FOUND, null, inputName);
        }

        String apiKey = HypixelApiUtil.getApiKey();
        if (apiKey.equals("N/A")) {
            return result.failure(INVALID_API, null, null);
        }

        PlayerInfo info = Profile.getPlayerInfo(inputName.toLowerCase());
        if (info == null) {
            return result.failure(PLAYER_NOT_FOUND, null, inputName);
        }

        try {
            String url = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + info.uuid;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            InputStreamReader reader = status >= 200 && status < 300
                    ? new InputStreamReader(conn.getInputStream())
                    : new InputStreamReader(conn.getErrorStream());

            JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
            if (!response.has("success") || !response.get("success").getAsBoolean()) {
                String cause = response.has("cause") && !response.get("cause").isJsonNull()
                        ? response.get("cause").getAsString()
                        : "Unknown error";
                String lower = cause.toLowerCase();
                if (lower.contains("invalid") || lower.contains("api key")) {
                    return result.failure(INVALID_API, null, info.name);
                }
                return result.failure(cause, null, info.name);
            }

            JsonElement playerElement = response.get("player");
            if (playerElement == null || playerElement.isJsonNull()) {
                return result.failure(PLAYER_NOT_FOUND, null, info.name);
            }

            return result.success(playerElement.getAsJsonObject(), info.name);
        } catch (Exception e) {
            return result.failure(e.getClass().getSimpleName(), e, info.name);
        }
    }

    public static void sendError(result result) {
        if (INVALID_API.equals(result.errorCode)) {
            Chat.send(Messages.INVALID_API);
        } else if (PLAYER_NOT_FOUND.equals(result.errorCode)) {
            Chat.send(Messages.PLAYER_NOT_FOUND);
        } else {
            Chat.send(Messages.FETCH_ERROR + result.properName + "§7| " + result.errorCode);
        }
    }

    public static final class result {
        public final boolean success;
        public final JsonObject player;
        public final String properName;
        public final String errorCode;
        public final Exception exception;

        private result(boolean success, JsonObject player, String properName, String errorCode, Exception exception) {
            this.success = success;
            this.player = player;
            this.properName = properName;
            this.errorCode = errorCode;
            this.exception = exception;
        }

        public static result success(JsonObject player, String properName) {
            return new result(true, player, properName, null, null);
        }

        public static result failure(String errorCode, Exception exception, String properName) {
            return new result(false, null, properName, errorCode, exception);
        }
    }
}