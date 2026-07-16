package com.konayuki.statflex.update;

import com.konayuki.statflex.statflex;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.HttpSecureConnection;
import com.konayuki.statflex.utils.Toggles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class Update {

    private static final String REPO = "Konayukiw/statflex";
    private static final String API_LATEST =
            "https://api.github.com/repos/" + REPO + "/releases/latest";

    private static final String JAR_PREFIX = "statflex-";

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
        }, "statflex-Update").start();
    }

    private static void checkForUpdates() throws Exception {
        File mcDir = Minecraft.getMinecraft().mcDataDir;
        URLConnection raw = new URL(API_LATEST).openConnection();
        if (raw instanceof HttpsURLConnection) {
            if (Toggles.ignoreCertificates) {
                HttpSecureConnection.trustAllCertificates((HttpsURLConnection) raw);
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
        String currentVersion = statflex.VERSION;

        if (!isNewer(latestVersion, currentVersion)) return;

        JsonObject asset = findStatflexJarAsset(root);
        if (asset == null) {
            Debug.error("statflex jar not found.");
            return;
        }

        String downloadUrl = asset.get("browser_download_url").getAsString();
        String fileName = asset.get("name").getAsString();

        File updateDir = new File(mcDir, "updates");
        File outFile = new File(updateDir, fileName);

        download(downloadUrl, outFile);

        updateAvailable = true;
        updateDownloaded = true;
        downloadedFile = outFile;
    }

    private static JsonObject findStatflexJarAsset(JsonObject root) {
        if (!root.has("assets") || !root.get("assets").isJsonArray()) {
            return null;
        }

        for (com.google.gson.JsonElement element : root.getAsJsonArray("assets")) {
            JsonObject asset = element.getAsJsonObject();
            String name = asset.get("name").getAsString().toLowerCase();

            if (name.startsWith(JAR_PREFIX) && name.endsWith(".jar")) {
                return asset;
            }
        }
        return null;
    }

    public static void prepareAndRunUpdateChecker(File newJar) throws IOException {
        File mcDir = Minecraft.getMinecraft().mcDataDir;
        File updateDir = new File(mcDir, "updates");
        if (!updateDir.exists()) updateDir.mkdirs();

        File info = new File(updateDir, "statflex-update.json");

        String finalName = newJar.getName();
        JsonObject obj = new JsonObject();
        obj.addProperty("newFile", newJar.getName());
        obj.addProperty("finalName", finalName);
        obj.addProperty("mcDir", mcDir.getAbsolutePath().replace("\\", "/"));

        try (Writer w = new OutputStreamWriter(new FileOutputStream(info), "UTF-8")) {
            w.write(obj.toString());
        }

        File bat = new File(updateDir, "statflex-updater.bat");
        writeBat(bat);

        Runtime.getRuntime().exec(new String[]{
                "cmd.exe", "/C", "start", "\"\"", "\"" + bat.getAbsolutePath() + "\""
        });

        Minecraft.getMinecraft().shutdown();
    }

    private static void writeBat(File bat) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(bat), "UTF-8"))) {

            pw.println("@echo off");
            pw.println("chcp 65001 > nul");
            pw.println("setlocal");
            pw.println("");
            pw.println("rem Bring console window to front");
            pw.println("powershell -NoProfile -ExecutionPolicy Bypass -Commands ^");
            pw.println("  \"Add-Type @'");
            pw.println("    using System;");
            pw.println("    using System.Runtime.InteropServices;");
            pw.println("    public class WinAPI {");
            pw.println("      [DllImport(\\\"user32.dll\\\")] public static extern bool SetForegroundWindow(IntPtr hWnd);");
            pw.println("      [DllImport(\\\"kernel32.dll\\\")] public static extern IntPtr GetConsoleWindow();");
            pw.println("    }");
            pw.println("'@; [WinAPI]::SetForegroundWindow([WinAPI]::GetConsoleWindow())\"");
            pw.println("");
            pw.println("rem Base");
            pw.println("cd /d \"%~dp0\"");
            pw.println("set \"BASE_DIR=%CD%\"");
            pw.println("set \"INFO=%BASE_DIR%\\statflex-update.json\"");
            pw.println("");
            pw.println("if not exist \"%INFO%\" (");
            pw.println("  echo update info not found");
            pw.println("  exit /b");
            pw.println(")");
            pw.println("");
            pw.println("rem Observing javaw.exe");
            pw.println(":wait");
            pw.println("tasklist | findstr /i \"javaw.exe\" >nul");
            pw.println("if not errorlevel 1 (");
            pw.println("  timeout /t 1 >nul");
            pw.println("  goto wait");
            pw.println(")");
            pw.println("");
            pw.println("rem PowerShell");
            pw.println("powershell -NoProfile -ExecutionPolicy Bypass -Commands ^");
            pw.println("  \"$info = Get-Content -Raw -Encoding UTF8 '%INFO%' | ConvertFrom-Json; \" ^");
            pw.println("  \"$mods = Join-Path $info.mcDir 'mods'; \" ^");
            pw.println("  \"$src  = Join-Path '%BASE_DIR%' $info.newFile; \" ^");
            pw.println("  \"$dst  = Join-Path $mods $info.finalName; \" ^");
            pw.println("  \"New-Item -ItemType Directory -Force -Path $mods | Out-Null; \" ^");
            pw.println("  \"Move-Item -Force $src $dst; \" ^");
            pw.println("  \"Get-ChildItem $mods -Filter 'statflex*.jar' | Where-Object { $_.Name -ne $info.finalName } | Remove-Item -Force\"");
            pw.println("");
            pw.println("rem Cleanup");
            pw.println("del \"%INFO%\"");
            pw.println("");
            pw.println("echo.");
            pw.println("echo statflex has been updated successfully.");
            pw.println("echo You can now launch Minecraft again.");
            pw.println("echo.");
            pw.println("pause");
        }
    }

    private static boolean isNewer(String latest, String current) {
        String nl = normalize(latest);
        String nc = normalize(current);

        String[] l = nl.split("\\.");
        String[] c = nc.split("\\.");

        int len = Math.max(l.length, c.length);
        for (int i = 0; i < len; i++) {
            int li = i < l.length ? parseSafe(l[i]) : 0;
            int ci = i < c.length ? parseSafe(c[i]) : 0;

            if (li > ci) return true;
            if (li < ci) return false;
        }
        return false;
    }

    private static String normalize(String v) {
        return v.replaceAll("[^0-9.]", "");
    }

    private static int parseSafe(String s) {
        if (s.isEmpty()) return 0;
        return Integer.parseInt(s);
    }

    public static void prepareUpdateAndExit() {
        if (!updateDownloaded || downloadedFile == null) {
            return;
        }

        try {
            prepareAndRunUpdateChecker(downloadedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum UpdateState {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        ERROR
    }

    public static UpdateState checkNow() {
        try {
            checkForUpdates();
            return updateDownloaded
                    ? UpdateState.UPDATE_AVAILABLE
                    : UpdateState.UP_TO_DATE;
        } catch (Exception e) {
            e.printStackTrace();
            return UpdateState.ERROR;
        }
    }

    private static void download(String url, File out) throws IOException {
        File parent = out.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        URLConnection raw = new URL(url).openConnection();
        if (raw instanceof HttpsURLConnection && Toggles.ignoreCertificates) {
            try {
                HttpSecureConnection.trustAllCertificates((HttpsURLConnection) raw);
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
