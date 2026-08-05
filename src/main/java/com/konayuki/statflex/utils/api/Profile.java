package com.konayuki.statflex.utils.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.Connection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class Profile {
    private Profile() {
    }

    public static PlayerInfo getPlayerInfo(String name) {
        LookupResult result = lookup(name);
        return result.info;
    }

    /**
     * @return {@link Boolean#TRUE} if the name is a real Mojang account,
     *         {@link Boolean#FALSE} if it definitely does not exist,
     *         {@code null} if the lookup failed (network/API error).
     */
    public static Boolean nameExists(String name) {
        LookupResult result = lookup(name);
        switch (result.status) {
            case FOUND:
                return Boolean.TRUE;
            case NOT_FOUND:
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    private static LookupResult lookup(String name) {
        if (name == null || name.isEmpty()) {
            return LookupResult.notFound();
        }

        try {
            URL url = new URL("https://crafthead.net/profile/" + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Connection.applyIfIgnoringCertificates(conn);

            conn.setRequestMethod("GET");
            conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

            int status = conn.getResponseCode();
            if (status == 404) {
                return LookupResult.notFound();
            }
            if (status < 200 || status >= 300) {
                return LookupResult.error();
            }

            InputStream stream = conn.getInputStream();
            if (stream == null) {
                return LookupResult.error();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
                if (json.has("error")) {
                    return LookupResult.notFound();
                }
                if (!json.has("id") || !json.has("name")) {
                    return LookupResult.notFound();
                }

                return LookupResult.found(new PlayerInfo(
                        json.get("id").getAsString(),
                        json.get("name").getAsString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return LookupResult.error();
        }
    }

    private enum Status {
        FOUND,
        NOT_FOUND,
        ERROR
    }

    private static final class LookupResult {
        final Status status;
        final PlayerInfo info;

        private LookupResult(Status status, PlayerInfo info) {
            this.status = status;
            this.info = info;
        }

        static LookupResult found(PlayerInfo info) {
            return new LookupResult(Status.FOUND, info);
        }

        static LookupResult notFound() {
            return new LookupResult(Status.NOT_FOUND, null);
        }

        static LookupResult error() {
            return new LookupResult(Status.ERROR, null);
        }
    }

    public static final class PlayerInfo {
        public final String uuid;
        public final String name;

        public PlayerInfo(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
