package com.konayuki.statflex.utils;

import com.konayuki.statflex.utils.chat.Chat;

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

    private Toggles() {
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
        return !disableHypixelFeaturesOutsideHypixel || ServerUtil.isHypixelServer();
    }

    public static void setIgnoreCertificates(boolean ignore) {
        ignoreCertificates = ignore;
    }

    public static void toggleListStats(boolean silent) {
        listStats = !listStats;
        Settings.getInstance().listStatsEnabled = listStats;
        Settings.save();
        if (!silent) {
            Chat.send(listStats
                    ? "§8[§cS§8]§7 Stats list has been §b§lEnabled"
                    : "§8[§cS§8]§7 Stats list has been §c§lDisabled");
        }
    }

    public static void toggleSkywarsListStats(boolean silent) {
        skywarsListStats = !skywarsListStats;
        Settings.getInstance().skywarsListStatsEnabled = skywarsListStats;
        Settings.save();
        if (!silent) {
            Chat.send(skywarsListStats
                    ? "§8[§cS§8]§7 Skywars stats list has been §b§lEnabled"
                    : "§8[§cS§8]§7 Skywars stats list has been §c§lDisabled");
        }
    }

    public static void toggleAutoStats(boolean silent) {
        autoStats = !autoStats;
        Settings.getInstance().autoStatsEnabled = autoStats;
        Settings.save();
        if (!silent) {
            Chat.send(autoStats
                    ? "§8[§cS§8]§7 Duels Auto-Stats has been §b§lEnabled"
                    : "§8[§cS§8]§7 Duels Auto-Stats has been §c§lDisabled");
        }
    }

    public static void toggleDenick(boolean silent) {
        denick = !denick;
        Settings.getInstance().denickEnabled = denick;
        Settings.save();
        if (!silent) {
            Chat.send(denick
                    ? "§8[§cS§8]§7 Denick has been §b§lEnabled"
                    : "§8[§cS§8]§7 Denick has been §c§lDisabled");
        }
    }

    public static void toggleDuelsUpdate(boolean silent) {
        duelsUpdated = !duelsUpdated;
        Settings.getInstance().duelsUpdated = duelsUpdated;
        Settings.save();
        if (!silent) {
            Chat.send(duelsUpdated
                    ? "§8[§cS§8]§7 Updated Duels Titles has been §b§lEnabled"
                    : "§8[§cS§8]§7 Updated Duels Titles has been §c§lDisabled");
        }
    }

    public static void toggleIgnoreCertificates(boolean silent) {
        ignoreCertificates = !ignoreCertificates;
        Settings.getInstance().ignoreCertificates = ignoreCertificates;
        Settings.save();
        if (!silent) {
            Chat.send(ignoreCertificates
                    ? "§8[§cS§8]§7 Secure connection has been §c§lDisabled"
                    : "§8[§cS§8]§7 Secure connection has been §b§lEnabled");
        }
    }

    public static void toggleKeepWho(boolean silent) {
        keepWho = !keepWho;
        Settings.getInstance().keepWhoEnabled = keepWho;
        Settings.save();
        if (!silent) {
            Chat.send(keepWho
                    ? "§8[§cS§8]§7 Original /who keeper has been §b§lEnabled"
                    : "§8[§cS§8]§7 Original /who keeper has been §c§lDisabled");
        }
    }

    public static void toggleDiscordRpc(boolean silent) {
        discordRpc = !discordRpc;
        Settings.getInstance().discordRpcEnabled = discordRpc;
        Settings.save();
        if (!silent) {
            Chat.send(discordRpc
                    ? "§8[§cS§8]§7 Discord RPC has been §b§lEnabled"
                    : "§8[§cS§8]§7 Discord RPC has been §c§lDisabled");
        }
    }

    public static void toggleDisableHypixelFeatures(boolean silent) {
        disableHypixelFeaturesOutsideHypixel = !disableHypixelFeaturesOutsideHypixel;
        Settings.getInstance().disableHypixelFeaturesOutsideHypixel = disableHypixelFeaturesOutsideHypixel;
        Settings.save();
        if (!silent) {
            Chat.send(disableHypixelFeaturesOutsideHypixel
                    ? "§8[§cS§8]§7 Auto-off outside Hypixel has been §b§lEnabled"
                    : "§8[§cS§8]§7 Auto-off outside Hypixel has been §c§lDisabled");
        }
    }
}
