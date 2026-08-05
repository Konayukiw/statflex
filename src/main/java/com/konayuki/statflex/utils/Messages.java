package com.konayuki.statflex.utils;

public final class Messages {
    public static final String PREFIX = Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY + "]" + Color.GRAY + " ";

    public static final String USAGE = PREFIX + "Invalid command. /s help for usage.";
    public static final String API_SET = PREFIX + "API Key has been set.";
    public static final String INVALID_COMMAND = PREFIX + "Invalid command. /s help for help.";
    public static final String INVALID_API = PREFIX + "API Key is invalid or not set. /s api " + Color.YELLOW + "[API Key]" + Color.GRAY + " to set.";
    public static final String INVALID_MODE = PREFIX + "Invalid mode: ";
    public static final String PLAYER_NOT_FOUND = PREFIX + "Player not found or stats unavailable.";
    public static final String NAME_NOT_FOUND = PREFIX + "Name not found.";
    public static final String FETCH_ERROR = PREFIX + "Failed to fetch player stats: ";
    public static final String UNEXPECTED_ERROR = PREFIX + "Unexpected error.";
    public static final String BEDWARS_STATS = PREFIX + Color.RED + Color.BOLD + "Bed" + Color.WHITE + Color.BOLD + "Wars " + Color.GRAY + "stats | ";
    public static final String DUELS_STATS = PREFIX + Color.AQUA + Color.BOLD + "Duels " + Color.GRAY + "| ";
    public static final String SKYWARS_STATS = PREFIX + Color.AQUA + Color.BOLD + "Sky" + Color.YELLOW + Color.BOLD + "Wars " + Color.GRAY + "| ";
    public static final String NO_STATS = PREFIX + "No stats found for ";
    public static final String UNKNOWN_GAMEMODE = PREFIX + "Unknown gamemode.";

    private Messages() {
    }
}
