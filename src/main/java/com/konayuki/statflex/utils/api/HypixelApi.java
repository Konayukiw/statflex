package com.konayuki.statflex.utils.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.api.Profile.PlayerInfo;
import com.konayuki.statflex.utils.chat.Chat;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HypixelApi {
    public static final String INVALID_API = Messages.INVALID_API;
    public static final String PLAYER_NOT_FOUND = Messages.PLAYER_NOT_FOUND;
    public static final String NAME_NOT_FOUND = Messages.NAME_NOT_FOUND;

    private HypixelApi() {
    }

    public static Result fetch(String inputName) {
        if (inputName == null || inputName.isEmpty()) {
            return Result.failure(NAME_NOT_FOUND, null, inputName);
        }

        String apiKey = HypixelApiUtil.get();
        if (apiKey.equals("N/A")) {
            return Result.failure(INVALID_API, null, null);
        }

        PlayerInfo info = Profile.info(inputName.toLowerCase());
        if (info == null) {
            return Result.failure(NAME_NOT_FOUND, null, inputName);
        }

        try {
            String url = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + info.uuid;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();
            InputStreamReader reader = status >= 200 && status < 300
                    ? new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                    : new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8);

            JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
            if (!response.has("success") || !response.get("success").getAsBoolean()) {
                String cause = response.has("cause") && !response.get("cause").isJsonNull()
                        ? response.get("cause").getAsString()
                        : "Unknown error";
                String lower = cause.toLowerCase();
                if (lower.contains("invalid") || lower.contains("api key")) {
                    return Result.failure(INVALID_API, null, info.name);
                }
                return Result.failure(cause, null, info.name);
            }

            JsonElement playerElement = response.get("player");
            if (playerElement == null || playerElement.isJsonNull()) {
                return Result.failure(PLAYER_NOT_FOUND, null, info.name);
            }

            return Result.success(playerElement.getAsJsonObject(), info.name);
        } catch (Exception e) {
            return Result.failure(e.getClass().getSimpleName(), e, info.name);
        }
    }

    public static void error(Result result) {
        if (INVALID_API.equals(result.errorCode)) {
            Chat.send(Messages.INVALID_API);
        } else if (PLAYER_NOT_FOUND.equals(result.errorCode)
                || NAME_NOT_FOUND.equals(result.errorCode)) {
            Chat.send(Messages.PLAYER_NOT_FOUND);
        } else {
            Chat.send(Messages.FETCH_ERROR + result.properName + Color.GRAY + "| " + result.errorCode);
        }
    }

    public static final class Result {
        public final boolean success;
        public final JsonObject player;
        public final String properName;
        public final String errorCode;
        public final Exception exception;

        private Result(boolean success, JsonObject player, String properName, String errorCode, Exception exception) {
            this.success = success;
            this.player = player;
            this.properName = properName;
            this.errorCode = errorCode;
            this.exception = exception;
        }

        public static Result success(JsonObject player, String properName) {
            return new Result(true, player, properName, null, null);
        }

        public static Result failure(String errorCode, Exception exception, String properName) {
            return new Result(false, null, properName, errorCode, exception);
        }
    }
}