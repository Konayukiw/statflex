package com.konayuki.statflex.utils;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.hypixel.Hypixel;

public final class Toggle {
    public static boolean listStats;
    public static boolean skywarsListStats;
    public static boolean autoStats;
    public static boolean ignoreCertificates;
    public static boolean denick;
    public static boolean duelsUpdated;
    public static boolean keepWho;
    public static boolean disableHypixelFeaturesOutsideHypixel;
    public static boolean discordRpc;

    private static final String ENABLED = Color.AQUA.toString() + Color.BOLD + "Enabled";
    private static final String DISABLED = Color.RED.toString() + Color.BOLD + "Disabled";

    private Toggle() {
    }

    private static void announce(String label, boolean enabled) {
        Chat.send(Messages.PREFIX + label + " has been " + (enabled ? ENABLED : DISABLED));
    }

    public static void syncFromSettings(Setting setting) {
        listStats = setting.listStatsEnabled;
        skywarsListStats = setting.skywarsListStatsEnabled;
        autoStats = setting.autoStatsEnabled;
        ignoreCertificates = setting.ignoreCertificates;
        denick = setting.denickEnabled;
        duelsUpdated = setting.duelsUpdated;
        keepWho = setting.keepWhoEnabled;
        disableHypixelFeaturesOutsideHypixel = setting.disableHypixelFeaturesOutsideHypixel;
        discordRpc = setting.discordRpcEnabled;
    }

    public static boolean isListStats() {
        return listStats && isHypixelFeatureAllowed();
    }

    public static boolean isSkywarsListStats() {
        return skywarsListStats && isHypixelFeatureAllowed();
    }

    public static boolean isAutoStats() {
        return autoStats && isHypixelFeatureAllowed();
    }

    public static boolean isIgnoreCertificatesEnabled() {
        return ignoreCertificates;
    }

    public static boolean isKeepWho() {
        return keepWho;
    }

    public static boolean isDuelsUpdateEnabled() {
        return duelsUpdated;
    }

    public static boolean isDenickEnabled() {
        return denick && isHypixelFeatureAllowed();
    }

    public static boolean isDiscordRpcEnabled() {
        return discordRpc;
    }

    public static boolean isHypixelFeatureAllowed() {
        return !disableHypixelFeaturesOutsideHypixel || Hypixel.isHypixel();
    }

    public static void setIgnoreCertificates(boolean ignore) {
        ignoreCertificates = ignore;
    }

    public static void toggleBedwarsListStats(boolean silent) {
        listStats = !listStats;
        Setting.getInstance().listStatsEnabled = listStats;
        Setting.save();
        if (!silent) {
            announce("Stats list", listStats);
        }
    }

    public static void toggleSkywarsListStats(boolean silent) {
        skywarsListStats = !skywarsListStats;
        Setting.getInstance().skywarsListStatsEnabled = skywarsListStats;
        Setting.save();
        if (!silent) {
            announce("Skywars stats list", skywarsListStats);
        }
    }

    public static void toggleAutoStats(boolean silent) {
        autoStats = !autoStats;
        Setting.getInstance().autoStatsEnabled = autoStats;
        Setting.save();
        if (!silent) {
            announce("Duels Auto-Stats", autoStats);
        }
    }

    public static void toggleDenick(boolean silent) {
        denick = !denick;
        Setting.getInstance().denickEnabled = denick;
        Setting.save();
        if (!silent) {
            announce("Denick", denick);
        }
    }

    public static void toggleDuelsUpdate(boolean silent) {
        duelsUpdated = !duelsUpdated;
        Setting.getInstance().duelsUpdated = duelsUpdated;
        Setting.save();
        if (!silent) {
            announce("Updated Duels Titles", duelsUpdated);
        }
    }

    public static void toggleIgnoreCertificates(boolean silent) {
        ignoreCertificates = !ignoreCertificates;
        Setting.getInstance().ignoreCertificates = ignoreCertificates;
        Setting.save();
        if (!silent) {
            announce("Secure connection", !ignoreCertificates);
        }
    }

    public static void toggleKeepWho(boolean silent) {
        keepWho = !keepWho;
        Setting.getInstance().keepWhoEnabled = keepWho;
        Setting.save();
        if (!silent) {
            announce("Original /who keeper", keepWho);
        }
    }

    public static void toggleDiscordRpc(boolean silent) {
        discordRpc = !discordRpc;
        Setting.getInstance().discordRpcEnabled = discordRpc;
        Setting.save();
        if (!silent) {
            announce("Discord RPC", discordRpc);
        }
    }

    public static void toggleDisableHypixelFeatures(boolean silent) {
        disableHypixelFeaturesOutsideHypixel = !disableHypixelFeaturesOutsideHypixel;
        Setting.getInstance().disableHypixelFeaturesOutsideHypixel = disableHypixelFeaturesOutsideHypixel;
        Setting.save();
        if (!silent) {
            announce("Auto-off outside Hypixel", disableHypixelFeaturesOutsideHypixel);
        }
    }
}
