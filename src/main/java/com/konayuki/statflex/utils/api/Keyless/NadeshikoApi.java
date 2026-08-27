package com.konayuki.statflex.utils.api.Keyless;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NadeshikoApi implements Provider {

    private static final Pattern EMBEDDED_PLAYER = Pattern.compile(
            "playerData = JSON.parse\\(decodeURIComponent\\(\"(.*?)\"\\)\\)");

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    @Override
    public String displayName() {
        return "Nadeshiko";
    }

    @Override
    public String buildUrl(String uuid) {
        return "https://nadeshiko.io/player/" + uuid + "/network";
    }

    @Override
    public String userAgent() {
        return USER_AGENT;
    }

    @Override
    public JsonObject parsePlayer(String body) throws Exception {
        if (body == null) {
            return null;
        }

        Matcher matcher = EMBEDDED_PLAYER.matcher(body);
        if (!matcher.find()) {
            return null;
        }

        JsonObject player = new JsonParser()
                .parse(URLDecoder.decode(matcher.group(1), "UTF-8"))
                .getAsJsonObject();
        return player == null || player.entrySet().isEmpty() ? null : player;
    }
}
