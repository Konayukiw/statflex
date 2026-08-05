package com.konayuki.statflex.features.duels;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.hypixel.Ranks;
import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.utils.Messages;

import com.google.gson.JsonObject;

import net.minecraft.util.EnumChatFormatting;

import java.text.DecimalFormat;

public class Duels {

    public static void fetchStats(String inputName, String mode) {
        fetchStats(inputName, mode, false);
    }

    public static void fetchStats(String inputName, String mode, boolean auto) {
        new Thread(() -> {
            try {
                HypixelApi.result result = HypixelApi.Fetch(inputName);
                if (!result.success) {
                    HypixelApi.sendError(result);
                    return;
                }

                JsonObject player = result.player;
                String properName = result.properName;
                JsonObject stats = player.has("stats") && player.getAsJsonObject("stats").has("Duels")
                        ? player.getAsJsonObject("stats").getAsJsonObject("Duels")
                        : new JsonObject();

                int wins, losses;
                double wlr;

                if (mode != null) {
                    String key = getModeKey(mode);
                    if (key == null) {
                        Chat.send(Messages.INVALID_MODE + mode);
                        return;
                    }
                    wins = stats.has(key + "_wins") ? stats.get(key + "_wins").getAsInt() : 0;
                    losses = stats.has(key + "_losses") ? stats.get(key + "_losses").getAsInt() : 1;
                } else {
                    wins = stats.has("wins") ? stats.get("wins").getAsInt() : 0;
                    losses = stats.has("losses") ? stats.get("losses").getAsInt() : 1;
                }
                wlr = losses == 0 ? wins : (double) wins / losses;

                String coloredPlayerName = Ranks.getColoredPlayerName(player, properName);
                String formattedWins = getFormattedWins(wins);
                String coloredWLR = getColoredWLR(wlr);
                String modeDisplay = mode != null ? getModeDisplayName(mode.toLowerCase()) : "";
                if (modeDisplay == null || modeDisplay.isEmpty()) {
                    modeDisplay = mode != null ? mode : "";
                }
                Title title = findTitleOrNone(wins, mode == null);
                String modeAndTitle = title.format
                        + (modeDisplay.isEmpty() ? "" : modeDisplay + " ") + title.label;

                if (auto) {
                    Chat.send(String.format(Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY
                                    + "] %s %s " + Color.GRAY + "| Wins: %s " + Color.GRAY + "| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                } else {
                    if (mode == null) {
                        Chat.send(Messages.DUELS_STATS);
                    } else {
                        Chat.send(String.format(Messages.DUELS_STATS + Color.GRAY + "[" + Color.YELLOW + "%s"
                                + Color.GRAY + "]", modeDisplay));
                    }
                    Chat.send(String.format(Color.RED + " || %s %s " + Color.GRAY + "| Wins: %s "
                                    + Color.GRAY + "| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                }

            } catch (Exception e) {
                Chat.send(Messages.FETCH_ERROR + e.getClass().getSimpleName());
            }
        }, "Duels").start();
    }

    public static String detectMode(String mode) {
        if (mode == null) {
            return null;
        }
        String line = mode.toLowerCase();
        if (line.contains("blitzsg") || line.contains("blitz")) {
            return "blitz_duel";
        } else if (line.contains("bow spleef") || line.contains("tnt")) {
            return "bowspleef_duel";
        } else if (line.contains("bow")) {
            return "bow_duel";
        } else if (line.contains("spleef")) {
            return "spleef_duel";
        } else if (line.contains("boxing")) {
            return "boxing_duel";
        } else if (line.contains("bridge")) {
            return "bridge_duel";
        } else if (line.contains("classic")) {
            return "classic_duel";
        } else if (line.contains("combo")) {
            return "combo_duel";
        } else if (line.contains("megawalls") || line.contains("mw")) {
            return "mw_duel";
        } else if (line.contains("nodebuff")) {
            return "potion_duel";
        } else if (line.contains("op")) {
            return "op_duel";
        } else if (line.contains("parkour")) {
            return "parkour_duel";
        } else if (line.contains("skywars") || line.contains("sw")) {
            return "sw_duel";
        } else if (line.contains("sumo")) {
            return "sumo_duel";
        } else if (line.contains("uhc")) {
            return "uhc_duel";
        } else if (line.contains("rush") || line.contains("bedrush")) {
            return "bedwars_two_one_duels_rush";
        } else if (line.contains("bw") || line.contains("bedwars") || line.contains("bed")) {
            return "bedwars_two_one_duels";
        } else if (line.contains("quake") || line.contains("quakecraft")) {
            return "quake_duel";
        }
        return getModeKey(mode);
    }

    public static String getModeKey(String input) {
        if (input == null)
            return null;
        switch (input.toLowerCase()) {
            case "blitzsg":
            case "blitz":
            case "blitz_duel":
                return "blitz_duel";
            case "bow":
            case "bow_duel":
                return "bow_duel";
            case "bow spleef":
            case "tnt":
            case "bowspleef_duel":
                return "bowspleef_duel";
            case "spleef":
            case "spleef_duel":
                return "spleef_duel";
            case "boxing":
            case "boxing_duel":
                return "boxing_duel";
            case "bridge":
            case "bridge_duel":
                return "bridge_duel";
            case "classic":
            case "classic_duel":
                return "classic_duel";
            case "combo":
            case "combo_duel":
                return "combo_duel";
            case "megawalls":
            case "mw":
            case "mw_duel":
                return "mw_duel";
            case "nodebuff":
            case "potion_duel":
                return "potion_duel";
            case "op":
            case "op_duel":
                return "op_duel";
            case "parkour":
            case "parkour_duel":
                return "parkour_duel";
            case "skywars":
            case "sw":
            case "sw_duel":
                return "sw_duel";
            case "sumo":
            case "sumo_duel":
                return "sumo_duel";
            case "uhc":
            case "uhc_duel":
                return "uhc_duel";
            case "bw":
            case "bedwars":
            case "bed":
            case "bedwars_two_one_duels":
                return "bedwars_two_one_duels";
            case "rush":
            case "bedrush":
            case "bedwars_two_one_duels_rush":
                return "bedwars_two_one_duels_rush";
            case "quake":
            case "quakecraft":
            case "quake_duel":
                return "quake_duel";
            default:
                return null;
        }
    }

    public static String getModeDisplayName(String input) {
        if (input == null)
            return null;
        switch (input.toLowerCase()) {
            case "blitzsg":
            case "blitz":
            case "blitz_duel":
                return "BlitzSG";
            case "bow":
            case "bow_duel":
                return "Bow";
            case "bow spleef":
            case "tnt":
            case "bowspleef_duel":
                return "Bow Spleef";
            case "spleef":
            case "spleef_duel":
                return "Spleef";
            case "boxing":
            case "boxing_duel":
                return "Boxing";
            case "bridge":
            case "bridge_duel":
                return "Bridge";
            case "classic":
            case "classic_duel":
                return "Classic";
            case "combo":
            case "combo_duel":
                return "Combo";
            case "megawalls":
            case "mw":
            case "mw_duel":
                return "MegaWalls";
            case "nodebuff":
            case "potion_duel":
                return "NoDebuff";
            case "op":
            case "op_duel":
                return "OP";
            case "parkour":
            case "parkour_duel":
                return "Parkour";
            case "skywars":
            case "sw":
            case "sw_duel":
                return "SkyWars";
            case "sumo":
            case "sumo_duel":
                return "Sumo";
            case "uhc":
            case "uhc_duel":
                return "UHC";
            case "bw":
            case "bedwars":
            case "bed":
            case "bedwars_two_one_duels":
                return "Bedwars";
            case "rush":
            case "bedrush":
            case "bedwars_two_one_duels_rush":
                return "Bed Rush";
            case "quake":
            case "quakecraft":
            case "quake_duel":
                return "Quakecraft";
            default:
                return null;
        }
    }

    public static String getFormattedWins(int wins) {
        DecimalFormat formatter = new DecimalFormat("#,###");

        EnumChatFormatting color;
        if (wins >= 50000)
            color = Color.DARK_PURPLE;
        else if (wins >= 25000)
            color = Color.DARK_RED;
        else if (wins >= 10000)
            color = Color.RED;
        else if (wins >= 5000)
            color = Color.GOLD;
        else if (wins >= 2500)
            color = Color.YELLOW;
        else if (wins >= 1000)
            color = Color.WHITE;
        else
            color = Color.GRAY;

        return color + formatter.format(wins);
    }

    public static String getColoredWLR(double wlr) {
        DecimalFormat df = new DecimalFormat("#.##");

        EnumChatFormatting color;
        if (wlr >= 30)
            color = Color.DARK_PURPLE;
        else if (wlr >= 15)
            color = Color.DARK_RED;
        else if (wlr >= 10)
            color = Color.RED;
        else if (wlr >= 5)
            color = Color.GOLD;
        else if (wlr >= 2)
            color = Color.YELLOW;
        else if (wlr >= 1)
            color = Color.WHITE;
        else
            color = Color.GRAY;

        return color + df.format(wlr);
    }

    /** A Duels prestige title split into its formatting prefix and its plain label. */
    public static final class Title {
        public final String format;
        public final String label;

        Title(String format, String label) {
            this.format = format;
            this.label = label;
        }
    }

    private static final String BOLD_DIVINE = Color.LIGHT_PURPLE.toString() + Color.BOLD;
    private static final String BOLD_CELESTIAL = Color.AQUA.toString() + Color.BOLD;
    private static final String BOLD_GODLIKE = Color.DARK_PURPLE.toString() + Color.BOLD;
    private static final String BOLD_GRANDMASTER = Color.YELLOW.toString() + Color.BOLD;
    private static final String BOLD_LEGEND = Color.DARK_RED.toString() + Color.BOLD;
    private static final String BOLD_ASCENDED = Color.RED.toString() + Color.BOLD;
    private static final String MASTER = Color.DARK_GREEN.toString();
    private static final String DIAMOND = Color.DARK_AQUA.toString();
    private static final String GOLD_TITLE = Color.GOLD.toString();
    private static final String IRON = Color.WHITE.toString();
    private static final String ROOKIE = Color.DARK_GRAY.toString();

    private static final Object[][] TITLE_THRESHOLDS = {
            { 100000, BOLD_DIVINE, "DIVINE" },
            { 90000, BOLD_CELESTIAL, "CELESTIAL V" },
            { 80000, BOLD_CELESTIAL, "CELESTIAL IV" },
            { 70000, BOLD_CELESTIAL, "CELESTIAL III" },
            { 60000, BOLD_CELESTIAL, "CELESTIAL II" },
            { 50000, BOLD_CELESTIAL, "CELESTIAL" },
            { 44000, BOLD_GODLIKE, "Godlike V" },
            { 38000, BOLD_GODLIKE, "Godlike IV" },
            { 32000, BOLD_GODLIKE, "Godlike III" },
            { 26000, BOLD_GODLIKE, "Godlike II" },
            { 20000, BOLD_GODLIKE, "Godlike" },
            { 18000, BOLD_GRANDMASTER, "Grandmaster V" },
            { 16000, BOLD_GRANDMASTER, "Grandmaster IV" },
            { 14000, BOLD_GRANDMASTER, "Grandmaster III" },
            { 12000, BOLD_GRANDMASTER, "Grandmaster II" },
            { 10000, BOLD_GRANDMASTER, "Grandmaster" },
            { 9000, BOLD_LEGEND, "Legend V" },
            { 7600, BOLD_LEGEND, "Legend IV" },
            { 6400, BOLD_LEGEND, "Legend III" },
            { 5200, BOLD_LEGEND, "Legend II" },
            { 4000, BOLD_LEGEND, "Legend" },
            { 3600, MASTER, "Master V" },
            { 3200, MASTER, "Master IV" },
            { 2800, MASTER, "Master III" },
            { 2400, MASTER, "Master II" },
            { 2000, MASTER, "Master" },
            { 1800, DIAMOND, "Diamond V" },
            { 1600, DIAMOND, "Diamond IV" },
            { 1400, DIAMOND, "Diamond III" },
            { 1200, DIAMOND, "Diamond II" },
            { 1000, DIAMOND, "Diamond" },
            { 900, GOLD_TITLE, "Gold V" },
            { 800, GOLD_TITLE, "Gold IV" },
            { 700, GOLD_TITLE, "Gold III" },
            { 600, GOLD_TITLE, "Gold II" },
            { 500, GOLD_TITLE, "Gold" },
            { 440, IRON, "Iron V" },
            { 380, IRON, "Iron IV" },
            { 320, IRON, "Iron III" },
            { 260, IRON, "Iron II" },
            { 200, IRON, "Iron" },
            { 180, ROOKIE, "Rookie V" },
            { 160, ROOKIE, "Rookie IV" },
            { 140, ROOKIE, "Rookie III" },
            { 120, ROOKIE, "Rookie II" },
            { 100, ROOKIE, "Rookie" },
    };

    /** Shown by /s duels when the player has not reached the lowest title yet. */
    private static final Title NO_TITLE = new Title(Color.DARK_GRAY.toString(), "None");

    /** Returns the matching title, or {@code null} when the player has none yet. */
    public static Title findTitle(int wins, boolean isOverall) {
        int usedWins = isOverall ? wins : wins * 2;

        if (usedWins >= 200000) {
            int ascendedLevel = ((usedWins - 200000) / 20000) + 1;
            return ascendedLevel == 1
                    ? new Title(BOLD_ASCENDED, "ASCENDED")
                    : new Title(BOLD_ASCENDED, "ASCENDED " + toRoman(ascendedLevel));
        }

        for (Object[] entry : TITLE_THRESHOLDS) {
            if (usedWins >= (int) entry[0]) {
                return new Title((String) entry[1], (String) entry[2]);
            }
        }
        return null;
    }

    public static Title findTitleOrNone(int wins, boolean isOverall) {
        Title title = findTitle(wins, isOverall);
        return title != null ? title : NO_TITLE;
    }

    public static String getColoredTitle(int wins, boolean isOverall) {
        Title title = findTitle(wins, isOverall);
        return title == null ? "" : title.format + title.label;
    }

    private static String toRoman(int num) {
        int[] values = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
        String[] numerals = {
                "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
        };

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num -= values[i];
                sb.append(numerals[i]);
            }
        }
        return sb.toString();
    }
}
