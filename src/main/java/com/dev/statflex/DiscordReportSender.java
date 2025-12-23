package com.dev.statflex;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordReportSender {

    public static void sendReport(String cheater, String reason, String reporter) {
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:3000/report");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-API-Key", "statflex-api");

                String json = "{"
                        + "\"cheater\":\"" + cheater + "\","
                        + "\"reason\":\"" + reason + "\","
                        + "\"reported_by\":\"" + reporter + "\""
                        + "}";

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                System.out.println("[S] Discord report response: " + responseCode);

                if (responseCode != 200) {
                    InputStream err = conn.getErrorStream();
                    if (err != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(err));
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println("[S] ERROR: " + line);
                        }
                        br.close();
                    }
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static String buildJson(String cheaterName, String reason, String reporterName) {
        return "{"
                + "\"cheater\":\"" + escape(cheaterName) + "\","
                + "\"reason\":\"" + escape(reason) + "\","
                + "\"reported_by\":\"" + escape(reporterName) + "\","
                + "\"source\":\"minecraft_mod\""
                + "}";
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}