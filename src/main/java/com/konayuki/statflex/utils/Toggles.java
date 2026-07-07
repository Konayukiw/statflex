package com.konayuki.statflex.utils;

public final class Toggles {
    public static boolean listStatsEnabled;
    public static boolean autoStatsEnabled;
    public static boolean ignoreCertificates;
    public static boolean denickEnabled;
    public static boolean duelsUpdate;
    public static boolean keepWhoEnabled;

    private Toggles() {
    }

    public static void syncFromSettings(Settings settings) {
        listStatsEnabled = settings.listStatsEnabled;
        autoStatsEnabled = settings.autoStatsEnabled;
        ignoreCertificates = settings.ignoreCertificates;
        denickEnabled = settings.denickEnabled;
        duelsUpdate = settings.duelsUpdate;
        keepWhoEnabled = settings.keepWhoEnabled;
    }

    public static boolean isListStatsEnabled() {
        return listStatsEnabled;
    }

    public static boolean isAutoStatsEnabled() {
        return autoStatsEnabled;
    }

    public static boolean isIgnoreCertificatesEnabled() {
        return ignoreCertificates;
    }

    public static boolean isKeepWhoEnabled() {
        return keepWhoEnabled;
    }

    public static boolean isDuelsUpdateEnabled() {
        return duelsUpdate;
    }

    public static void setIgnoreCertificates(boolean ignore) {
        ignoreCertificates = ignore;
    }

    public static void toggleListStats(boolean silent) {
        listStatsEnabled = !listStatsEnabled;
        Settings.getInstance().listStatsEnabled = listStatsEnabled;
        Settings.save();
        if (!silent) {
            Chat.send(listStatsEnabled
                    ? "§8[§cS§8]§7 Stats list has been §b§lEnabled"
                    : "§8[§cS§8]§7 Stats list has been §c§lDisabled");
        }
    }

    public static void toggleAutoStats(boolean silent) {
        autoStatsEnabled = !autoStatsEnabled;
        Settings.getInstance().autoStatsEnabled = autoStatsEnabled;
        Settings.save();
        if (!silent) {
            Chat.send(autoStatsEnabled
                    ? "§8[§cS§8]§7 Duels Auto-Stats has been §b§lEnabled"
                    : "§8[§cS§8]§7 Duels Auto-Stats has been §c§lDisabled");
        }
    }

    public static void toggleDenick(boolean silent) {
        denickEnabled = !denickEnabled;
        Settings.getInstance().denickEnabled = denickEnabled;
        Settings.save();
        if (!silent) {
            Chat.send(denickEnabled
                    ? "§8[§cS§8]§7 Denick detection has been §b§lEnabled"
                    : "§8[§cS§8]§7 Denick detection has been §c§lDisabled");
        }
    }

    public static void toggleDuelsUpdate(boolean silent) {
        duelsUpdate = !duelsUpdate;
        Settings.getInstance().duelsUpdate = duelsUpdate;
        Settings.save();
        if (!silent) {
            Chat.send(duelsUpdate
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
        keepWhoEnabled = !keepWhoEnabled;
        Settings.getInstance().keepWhoEnabled = keepWhoEnabled;
        Settings.save();
        if (!silent) {
            Chat.send(keepWhoEnabled
                    ? "§8[§cS§8]§7 Original /who keeper has been §b§lEnabled"
                    : "§8[§cS§8]§7 Original /who keeper has been §c§lDisabled");
        }
    }
}
