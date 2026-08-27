package com.konayuki.statflex.utils.api.Keyless;

import com.google.gson.JsonObject;

public final class AbyssApi implements Provider {

    @Override
    public String displayName() {
        return "Abyss";
    }

    @Override
    public String buildUrl(String uuid) {
        return "http://api.abyssoverlay.com/player?uuid=" + uuid;
    }

    @Override
    public String userAgent() {
        return "node-ao/2.0.3";
    }

    @Override
    public JsonObject parsePlayer(String body) throws Exception {
        return Provider.parseHypixelFormat(body);
    }
}
