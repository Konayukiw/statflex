package com.konayuki.statflex.utils.api.Keyless;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public interface Provider {

    String displayName();

    String buildUrl(String uuid);

    String userAgent();

    JsonObject parsePlayer(String body) throws Exception;

    static JsonObject parseHypixelFormat(String body) throws Exception {
        if (body == null || body.isEmpty()) {
            return null;
        }
        JsonObject root = new JsonParser().parse(body).getAsJsonObject();
        boolean success = root.has("success")
                && root.get("success").isJsonPrimitive()
                && root.get("success").getAsBoolean();
        if (!success) {
            return null;
        }

        JsonElement playerElement = root.get("player");
        if (playerElement == null || !playerElement.isJsonObject()) {
            return null;
        }

        JsonObject player = playerElement.getAsJsonObject();
        return player.entrySet().isEmpty() ? null : player;
    }
}
