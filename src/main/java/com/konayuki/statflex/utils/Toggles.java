package com.konayuki.statflex.utils;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.hypixel.isHypixel;

public final class Toggles {
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

    private Toggles() {
    }

    private static void announce(String label, boolean enabled) {
        Chat.send(Messages.PREFIX + label + " has been " + (enabled ? ENABLED : DISABLED));
    }

    public static void syncFromSettings(Settings settings) {
        listStats = settings.listStatsEnabled;
        skywarsListStats = settings.skywarsListStatsEnabled;
        autoStats = settings.autoStatsEnabled;
        ignoreCertificates = settings.ignoreCertificates;
        denick = settings.denickEnabled;
        duelsUpdated = settings.duelsUpdated;
        keepWho = settings.keepWhoEnabled;
        disableHypixelFeaturesOutsideHypixel = settings.disableHypixelFeaturesOutsideHypixel;
        discordRpc = settings.discordRpcEnabled;
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
        return !disableHypixelFeaturesOutsideHypixel || isHypixel.isHypixelServer();
    }

    public static void setIgnoreCertificates(boolean ignore) {
        ignoreCertificates = ignore;
    }

    public static void toggleListStats(boolean silent) {
        listStats = !listStats;
        Settings.getInstance().listStatsEnabled = listStats;
        Settings.save();
        if (!silent) {
            announce("Stats list", listStats);
        }
    }

    public static void toggleSkywarsListStats(boolean silent) {
        skywarsListStats = !skywarsListStats;
        Settings.getInstance().skywarsListStatsEnabled = skywarsListStats;
        Settings.save();
        if (!silent) {
            announce("Skywars stats list", skywarsListStats);
        }
    }

    public static void toggleAutoStats(boolean silent) {
        autoStats = !autoStats;
        Settings.getInstance().autoStatsEnabled = autoStats;
        Settings.save();
        if (!silent) {
            announce("Duels Auto-Stats", autoStats);
        }
    }

    public static void toggleDenick(boolean silent) {
        denick = !denick;
        Settings.getInstance().denickEnabled = denick;
        Settings.save();
        if (!silent) {
            announce("Denick", denick);
        }
    }

    public static void toggleDuelsUpdate(boolean silent) {
        duelsUpdated = !duelsUpdated;
        Settings.getInstance().duelsUpdated = duelsUpdated;
        Settings.save();
        if (!silent) {
            announce("Updated Duels Titles", duelsUpdated);
        }
    }

    public static void toggleIgnoreCertificates(boolean silent) {
        ignoreCertificates = !ignoreCertificates;
        Settings.getInstance().ignoreCertificates = ignoreCertificates;
        Settings.save();
        if (!silent) {
            announce("Secure connection", !ignoreCertificates);
        }
    }

    public static void toggleKeepWho(boolean silent) {
        keepWho = !keepWho;
        Settings.getInstance().keepWhoEnabled = keepWho;
        Settings.save();
        if (!silent) {
            announce("Original /who keeper", keepWho);
        }
    }

    public static void toggleDiscordRpc(boolean silent) {
        discordRpc = !discordRpc;
        Settings.getInstance().discordRpcEnabled = discordRpc;
        Settings.save();
        if (!silent) {
            announce("Discord RPC", discordRpc);
        }
    }

    public static void toggleDisableHypixelFeatures(boolean silent) {
        disableHypixelFeaturesOutsideHypixel = !disableHypixelFeaturesOutsideHypixel;
        Settings.getInstance().disableHypixelFeaturesOutsideHypixel = disableHypixelFeaturesOutsideHypixel;
        Settings.save();
        if (!silent) {
            announce("Auto-off outside Hypixel", disableHypixelFeaturesOutsideHypixel);
        }
    }
}
