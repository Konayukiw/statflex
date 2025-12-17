package com.dev.statflex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class Updater {

    private static final String REPO = "Konayukiw/statflex";
    private static final String API_LATEST =
            "https://api.github.com/repos/" + REPO + "/releases/latest";

    private static final String JAR_PREFIX = "statflex";

    public static boolean updateAvailable = false;
    public static boolean updateDownloaded = false;
    public static String latestVersion = "";
    public static File downloadedFile = null;

    public static void checkForUpdatesAsync() {
        new Thread(() -> {
            try {
                checkForUpdates();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "StatFlex-Updater").start();
    }

    private static void checkForUpdates() throws Exception {
        URLConnection raw = new URL(API_LATEST).openConnection();
        if (raw instanceof HttpsURLConnection) {
            if (Fetcher.ignoreCertificates) {
                GetUUID.trustAllCertificates((HttpsURLConnection) raw);
            }
        }
        HttpURLConnection conn = (HttpURLConnection) raw;
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "application/vnd.github+json");

        if (conn.getResponseCode() != 200) return;

        String json = readAll(conn.getInputStream());
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();

        latestVersion = root.get("tag_name").getAsString();
        String currentVersion = SFlex.VERSION;

        if (!isNewer(latestVersion, currentVersion)) return;

        JsonObject asset = root.getAsJsonArray("assets")
                .get(0).getAsJsonObject();

        String downloadUrl = asset.get("browser_download_url").getAsString();
        String fileName = asset.get("name").getAsString();

        File modsDir = new File(Minecraft.getMinecraft().mcDataDir, "mods");
        File outFile = new File(modsDir, fileName);

        download(downloadUrl, outFile);

        updateAvailable = true;
        updateDownloaded = true;
        downloadedFile = outFile;

    }

    private static boolean isNewer(String latest, String current) {
        String[] l = latest.replace("v", "").split("\\.");
        String[] c = current.replace("v", "").split("\\.");

        int len = Math.max(l.length, c.length);
        for (int i = 0; i < len; i++) {
            int li = i < l.length ? Integer.parseInt(l[i]) : 0;
            int ci = i < c.length ? Integer.parseInt(c[i]) : 0;
            if (li > ci) return true;
            if (li < ci) return false;
        }
        return false;
    }

    private static void download(String url, File out) throws IOException {
        URLConnection raw = new URL(url).openConnection();
        if (raw instanceof HttpsURLConnection && Fetcher.ignoreCertificates) {
            try {
                GetUUID.trustAllCertificates((HttpsURLConnection) raw);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HttpURLConnection conn = (HttpURLConnection) raw;
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (InputStream in = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(out)) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
    }


    private static String readAll(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
