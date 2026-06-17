package com.konayuki.statflex.system;

public final class Messages {
    public static final String PREFIX = "§8[§cS§8]§7 ";

    public static final String USAGE = PREFIX + "Invalid command. /s help for usage.";
    public static final String API_SET = PREFIX + "API Key has been set.";
    public static final String INVALID_COMMAND = PREFIX + "Invalid command. /s help for help.";
    public static final String INVALID_API = PREFIX + "API Key is invalid or not set. Use /s api [API Key] to set.";
    public static final String INVALID_MODE = PREFIX + "Invalid mode: ";
    public static final String PLAYER_NOT_FOUND = PREFIX + "Player not found or stats unavailable.";
    public static final String FRESH_STATS = PREFIX + "Fresh stats";
    public static final String FETCH_ERROR = PREFIX + "Failed to fetch player stats: ";
    public static final String UNEXPECTED_ERROR = PREFIX + "Unexpected error.";
    public static final String BEDWARS_STATS = PREFIX + "§c§lBed§f§lWars §7stats | ";
    public static final String DUELS_STATS = PREFIX + "§b§lDuels §7| ";
    public static final String SKYWARS_STATS = PREFIX + "§b§lSky§e§lWars §7| ";
    public static final String UNKNOWN_GAMEMODE = PREFIX + "Failed to execute Auto-Stats. Unknown gamemode. ";

    private Messages() {
    }
}
