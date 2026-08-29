package com.konayuki.statflex.features.tab;

import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TabStatsCache {

    public enum Game {
        BEDWARS, SKYWARS, DUELS
    }

    public static final class Snapshot {
        public final Game game;
        public final String header;
        public final List<String> lines;
        public final String footer;

        Snapshot(Game game, String header, List<String> lines, String footer) {
            this.game = game;
            this.header = header;
            this.lines = Collections.unmodifiableList(new ArrayList<String>(lines));
            this.footer = footer;
        }
    }

    private static volatile Snapshot snapshot;
    private static volatile int scrollIndex = 0;

    private TabStatsCache() {
    }

    public static boolean isTabMode(Game game) {
        Setting setting = Setting.get();
        switch (game) {
            case BEDWARS:
                return "Tab".equalsIgnoreCase(setting.bedwarsListDisplay);
            case SKYWARS:
                return "Tab".equalsIgnoreCase(setting.skywarsListDisplay);
            case DUELS:
                return "Tab".equalsIgnoreCase(setting.duelsListDisplay);
            default:
                return false;
        }
    }

    public static String header(Game game) {
        String raw;
        switch (game) {
            case BEDWARS:
                raw = Messages.BEDWARS_STATS;
                break;
            case SKYWARS:
                raw = Messages.SKYWARS_STATS;
                break;
            case DUELS:
                raw = Messages.DUELS_STATS;
                break;
            default:
                raw = Messages.PREFIX;
                break;
        }
        String cleaned = raw.trim();
        if (cleaned.endsWith("|")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    public static void set(Game game, String header, List<String> lines, String footer) {
        snapshot = new Snapshot(game, header, lines, footer);
        scrollIndex = 0;
    }

    public static void begin(Game game) {
        Snapshot current = snapshot;
        if (current != null && current.game == game) {
            snapshot = null;
            scrollIndex = 0;
        }
    }

    public static synchronized void append(Game game, String header, String line) {
        Snapshot current = snapshot;
        if (current == null || current.game != game) {
            List<String> lines = new ArrayList<String>();
            lines.add(line);
            snapshot = new Snapshot(game, header, lines, null);
            scrollIndex = 0;
        } else {
            List<String> lines = new ArrayList<String>(current.lines);
            lines.add(line);
            snapshot = new Snapshot(game, current.header, lines, current.footer);
        }
    }

    public static Snapshot get() {
        return snapshot;
    }

    public static void clear() {
        snapshot = null;
        scrollIndex = 0;
    }

    public static int scrollIndex() {
        return scrollIndex;
    }

    public static void setScrollIndex(int index) {
        scrollIndex = index;
    }
}
