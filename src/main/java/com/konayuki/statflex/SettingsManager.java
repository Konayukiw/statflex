package com.konayuki.statflex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SettingsManager {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File configFile = new File("config/sfsettings.json");

    public boolean denickEnabled = false;
    public boolean listStatsEnabled = true;
    public boolean autoStatsEnabled = true;
    public boolean duelsUpdate = false;
    public boolean ignoreCertificates = false;
    public boolean keepWhoEnabled = false;
    public String apiKey = "";
    public String skinSaveDir = "";
    public int warnLevel = 0;
    public double warnFKDR = 0.0;
    public double flagInterval = 5.0;

    private static SettingsManager instance;

    public static SettingsManager getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (!configFile.exists()) {
            instance = new SettingsManager();
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            instance = gson.fromJson(reader, SettingsManager.class);
            if (instance == null) {
                instance = new SettingsManager();
            }
        } catch (IOException e) {
            e.printStackTrace();
            instance = new SettingsManager();
        }
    }

    public static void save() {
        try {
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(getInstance(), writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getFlagInterval() {
        return flagInterval;
    }

    public void setFlagInterval(double seconds) {
        this.flagInterval = Math.max(0.0, Math.min(20.0, seconds));
        save();
    }

    public String[] autoGGMessages = new String[0];

    public File getSkinSaveDir() {
        if (skinSaveDir == null || skinSaveDir.isEmpty()) {
            return new File(net.minecraft.client.Minecraft.getMinecraft().mcDataDir, "downloads");
        }
        return new File(skinSaveDir);
    }

    public void setSkinSaveDir(File dir) {
        this.skinSaveDir = dir.getAbsolutePath();
        save();
    }
}
