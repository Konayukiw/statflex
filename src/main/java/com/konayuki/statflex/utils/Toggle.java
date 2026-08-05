package com.konayuki.statflex.utils;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.hypixel.Server;

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

    public static void sync(Setting setting) {
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

    public static boolean isAllowed() {
        return !disableHypixelFeaturesOutsideHypixel || Server.isHypixel();
    }

    public static boolean isBwList() {
        return listStats && isAllowed();
    }

    public static boolean isSwList() {
        return skywarsListStats && isAllowed();
    }

    public static boolean isAuto() {
        return autoStats && isAllowed();
    }

    public static boolean isDenick() {
        return denick && isAllowed();
    }

    public static boolean isNewDuels() {
        return duelsUpdated;
    }

    public static boolean isKeepWho() {
        return keepWho;
    }

    public static boolean isRpc() {
        return discordRpc;
    }

    public static boolean isInsecure() {
        return ignoreCertificates;
    }

    public static void flipBwList(boolean silent) {
        listStats = !listStats;
        Setting.get().listStatsEnabled = listStats;
        Setting.save();
        if (!silent) {
            announce("Stats list", listStats);
        }
    }

    public static void flipSwList(boolean silent) {
        skywarsListStats = !skywarsListStats;
        Setting.get().skywarsListStatsEnabled = skywarsListStats;
        Setting.save();
        if (!silent) {
            announce("Skywars stats list", skywarsListStats);
        }
    }

    public static void flipAuto(boolean silent) {
        autoStats = !autoStats;
        Setting.get().autoStatsEnabled = autoStats;
        Setting.save();
        if (!silent) {
            announce("Duels Auto-Stats", autoStats);
        }
    }

    public static void flipDenick(boolean silent) {
        denick = !denick;
        Setting.get().denickEnabled = denick;
        Setting.save();
        if (!silent) {
            announce("Denick", denick);
        }
    }

    public static void flipNewDuels(boolean silent) {
        duelsUpdated = !duelsUpdated;
        Setting.get().duelsUpdated = duelsUpdated;
        Setting.save();
        if (!silent) {
            announce("Updated Duels Titles", duelsUpdated);
        }
    }

    public static void flipKeepWho(boolean silent) {
        keepWho = !keepWho;
        Setting.get().keepWhoEnabled = keepWho;
        Setting.save();
        if (!silent) {
            announce("Original /who keeper", keepWho);
        }
    }

    public static void flipRpc(boolean silent) {
        discordRpc = !discordRpc;
        Setting.get().discordRpcEnabled = discordRpc;
        Setting.save();
        if (!silent) {
            announce("Discord RPC", discordRpc);
        }
    }

    public static void flipInsecure(boolean silent) {
        ignoreCertificates = !ignoreCertificates;
        Setting.get().ignoreCertificates = ignoreCertificates;
        Setting.save();
        if (!silent) {
            announce("Secure connection", !ignoreCertificates);
        }
    }

    public static void flipGate(boolean silent) {
        disableHypixelFeaturesOutsideHypixel = !disableHypixelFeaturesOutsideHypixel;
        Setting.get().disableHypixelFeaturesOutsideHypixel = disableHypixelFeaturesOutsideHypixel;
        Setting.save();
        if (!silent) {
            announce("Auto-off outside Server", disableHypixelFeaturesOutsideHypixel);
        }
    }

    public static void setInsecure(boolean ignore) {
        ignoreCertificates = ignore;
    }

    private static void announce(String label, boolean enabled) {
        Chat.send(Messages.PREFIX + label + " has been " + (enabled ? ENABLED : DISABLED));
    }
}
