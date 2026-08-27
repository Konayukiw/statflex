package com.konayuki.statflex.utils.api.Keyless;

import com.google.gson.JsonObject;
import com.konayuki.statflex.statflex;

public final class BordicApi implements Provider {

    private static final String ENDPOINT = "https://api.bordic.xyz/v3/cache/hypixel?uuid=";

    @Override
    public String displayName() {
        return "Bordic";
    }

    @Override
    public String buildUrl(String uuid) {
        return ENDPOINT + uuid;
    }

    @Override
    public String userAgent() {
        return "statflex/" + statflex.VERSION;
    }

    @Override
    public JsonObject parsePlayer(String body) throws Exception {
        return Provider.parseHypixelFormat(body);
    }
}
