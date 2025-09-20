package com.dev.statflex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class GetUUID {

    public static class PlayerInfo {
        public final String uuid;
        public final String name;

        public PlayerInfo(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    public static void setIgnoreCertificates(boolean ignore) {
        Fetcher.ignoreCertificates = ignore;
    }

    public static void trustAllCertificates(HttpsURLConnection con) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        con.setSSLSocketFactory(sc.getSocketFactory());

        con.setHostnameVerifier((hostname, session) -> true);
    }

    public static PlayerInfo getPlayerInfo(String name) {
        try {
            URL url = new URL("https://crafthead.net/profile/" + name);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            if (Fetcher.ignoreCertificates && con instanceof HttpsURLConnection) {
                HttpsURLConnection httpsCon = (HttpsURLConnection) con;
                trustAllCertificates(httpsCon);
            }

            con.setRequestMethod("GET");
            con.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                JsonParser parser = new JsonParser();
                JsonObject json = parser.parse(response.toString()).getAsJsonObject();

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

    public static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }

}
