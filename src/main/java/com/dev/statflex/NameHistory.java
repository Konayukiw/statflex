package com.dev.statflex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NameHistory {

    private static boolean ignoreCertificates = false;

    public static void fetchNameHistory(String inputName) {
        new Thread(() -> {
            try {
                GetUUID.PlayerInfo info = GetUUID.getPlayerInfo(inputName);
                if (info == null) {
                    sendChat("§8[§cS§8]§7 Player not found for name " + inputName);
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

                if (ignoreCertificates && connection instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                    try {
                        SSLContext sc = SSLContext.getInstance("SSL");
                        sc.init(null, new TrustManager[] {
                                new X509TrustManager() {
                                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                        return new java.security.cert.X509Certificate[0];
                                    }

                                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                            String authType) {
                                    }

                                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                            String authType) {
                                    }
                                }
                        }, new java.security.SecureRandom());
                        httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
                        httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    } catch (Exception e) {
                        sendChat("§8[§cS§8]§7 Failed to set ignoreCertificates: " + e.getClass().getSimpleName());
                    }
                }

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonParser parser = new JsonParser();
                JsonObject response = parser.parse(reader).getAsJsonObject();

                if (!response.has("success") || !response.get("success").getAsBoolean()) {
                    sendChat("§8[§cS§8]§7 Failed to fetch name history for " + properName);
                    return;
                }

                JsonObject data = response.getAsJsonObject("data");
                if (data == null || !data.has("usernames")) {
                    sendChat("§8[§cS§8]§7 No name history found for " + properName);
                    return;
                }

                JsonArray names = data.getAsJsonArray("usernames");
                if (names.size() == 0) {
                    sendChat("§8[§cS§8]§7 No name history found for " + properName);
                    return;
                }

                sendChat("§8[§cS§8] §b§lName History §7for " + Format.getColoredPlayerName(data, inputName) + " §7|");

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

                    sendChat(String.format("§8[§cS§8]§e %s §7| %s", name, changedAt));
                }

            } catch (Exception e) {
                sendChat("§8[§cS§8]§7 Failed to fetch name history: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }
        }).start();
    }

    private static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }
}
