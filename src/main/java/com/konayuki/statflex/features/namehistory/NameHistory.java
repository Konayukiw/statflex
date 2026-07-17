package com.konayuki.statflex.features.namehistory;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.Ranks;
import com.konayuki.statflex.utils.HttpSecureConnection;
import com.konayuki.statflex.utils.Messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.api.Profile;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class NameHistory {

    public static void fetchNameHistory(String inputName) {
        new Thread(() -> {
            try {
                Profile.PlayerInfo info = Profile.getPlayerInfo(inputName);
                if (info == null) {
                    Chat.send(Messages.PLAYER_NOT_FOUND + inputName);
                    return;
                }

                String properName = info.name;

                String urlStr = "https://api.crafty.gg/api/v2/players/" + inputName;
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

                // connection.setRequestProperty("Accept", "application/json, text/plain, */*");
                // connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                // connection.setRequestProperty("Referer", "https://crafty.gg/");

                HttpSecureConnection.applyIfIgnoringCertificates(connection);

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonParser parser = new JsonParser();
                JsonObject response = parser.parse(reader).getAsJsonObject();

                if (!response.has("success") || !response.get("success").getAsBoolean()) {
                    Chat.send("§8[§cS§8]§7 Failed to fetch name history for " + properName);
                    return;
                }

                JsonObject data = response.getAsJsonObject("data");
                if (data == null || !data.has("usernames")) {
                    Chat.send("§8[§cS§8]§7 No name history found for " + properName);
                    return;
                }

                JsonArray names = data.getAsJsonArray("usernames");
                if (names.size() == 0) {
                    Chat.send("§8[§cS§8]§7 No name history found for " + properName);
                    return;
                }

                Chat.send("§8[§cS§8] §b§lName History §7for " + Ranks.getColoredPlayerName(data, inputName) + " §7|");

                DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (int i = 0; i < names.size(); i++) {
                    JsonObject entry = names.get(i).getAsJsonObject();
                    String name = entry.has("username") ? entry.get("username").getAsString() : "Unknown";

                    String changedAt;
                    if (entry.has("changed_at") && !entry.get("changed_at").isJsonNull()) {
                        String iso = entry.get("changed_at").getAsString();
                        OffsetDateTime odt = OffsetDateTime.parse(iso, inputFormatter);
                        changedAt = odt.format(outputFormatter);
                    } else {
                        changedAt = "First Name";
                    }

                    Chat.send(String.format("§8[§cS§8]§e %s §7| %s", name, changedAt));
                }

            } catch (Exception e) {
                Chat.send("§8[§cS§8]§7 Failed to fetch name history: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
        }).start();
    }
}
