package com.konayuki.statflex.features.duels;

import com.konayuki.statflex.utils.Chat;
import com.konayuki.statflex.utils.HypixelApiUtils;
import com.konayuki.statflex.utils.ProfileUtils;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Format;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Duels {

    public static void fetchStats(String inputName, String mode) {
        fetchStats(inputName, mode, false);
    }

    public static void fetchStats(String inputName, String mode, boolean auto) {
        new Thread(() -> {
            try {
                String apiKey = HypixelApiUtils.getApiKey();
                if (apiKey.equals("N/A")) {
                    Chat.send(Messages.INVALID_API);
                    return;
                }

                ProfileUtils.PlayerInfo info = ProfileUtils.getPlayerInfo(inputName);
                if (info == null) {
                    Debug.log("Failed to fetch player stats: " + inputName);
                    return;
                }

                String uuid = info.uuid;
                String properName = info.name;

                String urlStr = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");

                int status = connection.getResponseCode();
                InputStreamReader reader;
                if (status >= 200 && status < 300) {
                    reader = new InputStreamReader(connection.getInputStream());
                } else {
                    reader = new InputStreamReader(connection.getErrorStream());
                }
                JsonParser parser = new JsonParser();
                JsonObject response = parser.parse(reader).getAsJsonObject();

                if (!response.has("success") || !response.get("success").getAsBoolean()) {
                    String cause = response.has("cause") && !response.get("cause").isJsonNull()
                            ? response.get("cause").getAsString()
                            : "Unknown error";

                    String lower = cause.toLowerCase();

                    if (lower.contains("invalid") || lower.contains("api key")) {
                        Chat.send(Messages.INVALID_API);
                    } else {
                        Chat.send(Messages.FETCH_ERROR + cause);
                    }
                    return;
                }

                JsonElement playerElement = response.get("player");
                if (playerElement == null || playerElement.isJsonNull()) {
                    Debug.log("Failed to fetch player stats: " + inputName);
                    return;
                }

                JsonObject player = playerElement.getAsJsonObject();
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

                String coloredPlayerName = Format.getColoredPlayerName(player, properName);
                String formattedWins = getFormattedWins(wins);
                String coloredWLR = getColoredWLR(wlr);
                String modeDisplay = mode != null ? getModeDisplayName(mode.toLowerCase()) : "";
                if (modeDisplay == null || modeDisplay.isEmpty()) {
                    modeDisplay = mode != null ? mode : "";
                }
                String rawTitle = mode != null ? getColoredTitle(wins, false) : getColoredTitle(wins, true);
                String title = (rawTitle == null || rawTitle.isEmpty()) ? "§8None" : rawTitle;
                String colorCode = extractColorCode(title);
                String titleBody = title.length() > 2 ? title.substring(2) : title;
                String modeAndTitle = colorCode + (modeDisplay.isEmpty() ? "" : modeDisplay + " ") + titleBody;

                if (auto) {
                    Chat.send(String.format("§8[§cS§8] %s %s §7| Wins: %s §7| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                } else {
                    if (mode == null) {
                        Chat.send(Messages.DUELS_STATS);
                    } else {
                        Chat.send(String.format(Messages.DUELS_STATS +  "§7[§e%s§7]", modeDisplay));
                    }
                    Chat.send(String.format("§c || %s %s §7| Wins: %s §7| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                }

            } catch (Exception e) {
                Chat.send(Messages.FETCH_ERROR + e.getClass().getSimpleName());
            }
        }).start();
    }

    public static String detectModeFromLocraw(String mode) {
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

        String color;
        if (wins >= 50000)
            color = "§5";
        else if (wins >= 25000)
            color = "§4";
        else if (wins >= 10000)
            color = "§c";
        else if (wins >= 5000)
            color = "§6";
        else if (wins >= 2500)
            color = "§e";
        else if (wins >= 1000)
            color = "§f";
        else
            color = "§7";

        return color + formatter.format(wins);
    }

    public static String getColoredWLR(double wlr) {
        DecimalFormat df = new DecimalFormat("#.##");

        String color;
        if (wlr >= 30)
            color = "§5";
        else if (wlr >= 15)
            color = "§4";
        else if (wlr >= 10)
            color = "§c";
        else if (wlr >= 5)
            color = "§6";
        else if (wlr >= 2)
            color = "§e";
        else if (wlr >= 1)
            color = "§f";
        else
            color = "§7";

        return color + df.format(wlr);
    }

    public static String getColoredTitle(int wins, boolean isOverall) {
        Object[][] thresholdsOverall = {
                { 100000, "§d§lDIVINE" },
                { 90000, "§b§lCELESTIAL V" },
                { 80000, "§b§lCELESTIAL IV" },
                { 70000, "§b§lCELESTIAL III" },
                { 60000, "§b§lCELESTIAL II" },
                { 50000, "§b§lCELESTIAL" },
                { 44000, "§5§lGodlike V" },
                { 38000, "§5§lGodlike IV" },
                { 32000, "§5§lGodlike III" },
                { 26000, "§5§lGodlike II" },
                { 20000, "§5§lGodlike" },
                { 18000, "§e§lGrandmaster V" },
                { 16000, "§e§lGrandmaster IV" },
                { 14000, "§e§lGrandmaster III" },
                { 12000, "§e§lGrandmaster II" },
                { 10000, "§e§lGrandmaster" },
                { 9000, "§4§lLegend V" },
                { 7600, "§4§lLegend IV" },
                { 6400, "§4§lLegend III" },
                { 5200, "§4§lLegend II" },
                { 4000, "§4§lLegend" },
                { 3600, "§2Master V" },
                { 3200, "§2Master IV" },
                { 2800, "§2Master III" },
                { 2400, "§2Master II" },
                { 2000, "§2Master" },
                { 1800, "§3Diamond V" },
                { 1600, "§3Diamond IV" },
                { 1400, "§3Diamond III" },
                { 1200, "§3Diamond II" },
                { 1000, "§3Diamond" },
                { 900, "§6Gold V" },
                { 800, "§6Gold IV" },
                { 700, "§6Gold III" },
                { 600, "§6Gold II" },
                { 500, "§6Gold" },
                { 440, "§fIron V" },
                { 380, "§fIron IV" },
                { 320, "§fIron III" },
                { 260, "§fIron II" },
                { 200, "§fIron" },
                { 180, "§8Rookie V" },
                { 160, "§8Rookie IV" },
                { 140, "§8Rookie III" },
                { 120, "§8Rookie II" },
                { 100, "§8Rookie" },
        };

        int usedWins = isOverall ? wins : wins * 2;

        if (usedWins >= 200000) {
            int ascendedLevel = ((usedWins - 200000) / 20000) + 1;
            if (ascendedLevel == 1) {
                return "§c§lASCENDED";
            } else {
                return "§c§lASCENDED " + toRoman(ascendedLevel);
            }
        }

        for (Object[] entry : thresholdsOverall) {
            int reqWins = (int) entry[0];
            String title = (String) entry[1];
            if (usedWins >= reqWins) {
                return title;
            }
        }
        return "";
    }

    public static String extractColorCode(String title) {
        if (title != null && title.startsWith("§")) {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < title.length() - 1; i++) {
                if (title.charAt(i) == '§') {
                    code.append(title.charAt(i)).append(title.charAt(i + 1));
                    i++;
                } else {
                    break;
                }
            }
            return code.toString();
        }
        return "§8";
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
