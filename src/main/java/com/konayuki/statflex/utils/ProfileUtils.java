package com.konayuki.statflex.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ProfileUtils {
    private ProfileUtils() {
    }

    public static PlayerInfo getPlayerInfo(String name) {
        try {
            URL url = new URL("https://crafthead.net/profile/" + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            HttpSecurityUtil.applyIfIgnoringCertificates(connection);

            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
                if (json.has("error")) {
                    return null;
                }

                return new PlayerInfo(
                        json.get("id").getAsString(),
                        json.get("name").getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
