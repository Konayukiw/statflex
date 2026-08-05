package com.konayuki.statflex.features.bedwars;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.hypixel.Ranks;
import com.konayuki.statflex.utils.Messages;

import com.google.gson.JsonObject;

import java.text.DecimalFormat;

public class Bedwars {

    public static void fetchStats(String inputName, String mode) {
        new Thread(() -> {
            try {
                HypixelApi.result result = HypixelApi.Fetch(inputName);
                if (!result.success) {
                    HypixelApi.sendError(result);
                    return;
                }

                JsonObject player = result.player;
                String properName = result.properName;
                JsonObject stats = player.has("stats") && player.get("stats").getAsJsonObject().has("Bedwars")
                        ? player.get("stats").getAsJsonObject().get("Bedwars").getAsJsonObject()
                        : new JsonObject();

                int level = player.has("achievements") && player.get("achievements").getAsJsonObject().has("bedwars_level")
                        ? player.get("achievements").getAsJsonObject().get("bedwars_level").getAsInt()
                        : 0;

                int finals, deaths;
                double fkdr;

                if (mode != null && !mode.isEmpty()) {
                    String key = getModeKey(mode);
                    if (key == null) {
                        Chat.send(Messages.INVALID_MODE + mode);
                        return;
                    }
                    finals = stats.has(key + "_final_kills_bedwars")
                            ? stats.get(key + "_final_kills_bedwars").getAsInt()
                            : 0;
                    deaths = stats.has(key + "_final_deaths_bedwars")
                            ? stats.get(key + "_final_deaths_bedwars").getAsInt()
                            : 1;
                    fkdr = deaths == 0 ? finals : (double) finals / deaths;
                } else {
                    finals = stats.has("final_kills_bedwars") ? stats.get("final_kills_bedwars").getAsInt() : 0;
                    deaths = stats.has("final_deaths_bedwars") ? stats.get("final_deaths_bedwars").getAsInt() : 1;
                    fkdr = deaths == 0 ? finals : (double) finals / deaths;
                }

                String coloredLevel = getColoredLevel(level);
                String coloredPlayerName = Ranks.getColoredPlayerName(player, properName);
                String formattedFinals = getFormattedFinals(finals);
                String coloredFKDR = getColoredFKDR(fkdr);

                if (mode != null && !mode.isEmpty()) {
                    String displayMode = getModeDisplayName(mode.toLowerCase());
                    if (displayMode == null) displayMode = mode;
                    Chat.send(String.format(Messages.BEDWARS_STATS + "§7[§e%s§7]", displayMode));
                    Chat.send(String.format("§c || %s %s §7| Finals: %s §7| FKDR: %s",
                            coloredLevel, coloredPlayerName, formattedFinals, coloredFKDR));
                } else {
                    Chat.send(Messages.BEDWARS_STATS);
                    Chat.send(String.format("§c || %s %s §7| Finals: %s §7| FKDR: %s",
                            coloredLevel, coloredPlayerName, formattedFinals, coloredFKDR));
                }

            } catch (Exception e) {
                Chat.send(Messages.FETCH_ERROR + e.getClass().getSimpleName());
            }
        }, "Bedwars").start();
    }

    public static String formatLevelPlain(int level) {
        return String.valueOf(level);
    }

    public static String formatFinalsPlain(int finals) {
        return new DecimalFormat("#,###").format(finals);
    }

    public static String formatFKDRPlain(double fkdr) {
        return new DecimalFormat("#.##").format(fkdr);
    }

    private static String getModeKey(String input) {
        switch (input.toLowerCase()) {
            case "solo":
            case "1s":
                return "eight_one";
            case "duos":
            case "doubles":
            case "2s":
                return "eight_two";
            case "threes":
            case "3s":
                return "four_three";
            case "fours":
            case "4s":
                return "four_four";
            case "4v4":
                return "two_four";
            case "castle":
                return "castle";
            case "armed":
                return "armed";
            case "swap":
                return "swap";
            case "ultimate":
            case "ult":
                return "ultimate";
            case "rush":
                return "rush";
            case "voidless":
                return "voidless";
            case "lucky":
                return "lucky_block";
            default:
                return null;
        }
    }

    private static String getModeDisplayName(String input) {
        switch (input.toLowerCase()) {
            case "solo":
            case "1s":
                return "Solo";
            case "duos":
            case "doubles":
            case "2s":
                return "Doubles";
            case "threes":
            case "3s":
                return "Threes";
            case "fours":
            case "4s":
                return "Fours";
            case "4v4":
                return "4v4";
            case "castle":
                return "Castle";
            case "armed":
                return "Armed";
            case "swap":
                return "Swap";
            case "ultimate":
            case "ult":
                return "Ultimate";
            case "rush":
                return "Rush";
            case "voidless":
                return "Voidless";
            case "lucky":
                return "Lucky";
            default:
                return null;
        }
    }

    public static String getColoredLevel(int level) {
        if (level >= 1000) {
            String levelStr = String.format("%04d", level);
            char[] digits = levelStr.toCharArray();
            StringBuilder sb = new StringBuilder();

            if (level < 1100) {
                String bracketLeft = "§c[";
                String bracketRight = "§5]";
                String[] digitColors = {"§6", "§e", "§a", "§b"};
                String symbolColor = "§d";
                String symbol = "✫";
                sb.append(bracketLeft);
                for (int i = 0; i < 4; i++) sb.append(digitColors[i]).append(digits[i]);
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();
            } else if (level < 1200) {
                String bracketLeft = "§7[";
                String bracketRight = "§7]";
                String digitColor = "§f";
                String symbolColor = "§7";
                String symbol = "✪";
                sb.append(bracketLeft);
                for (int i = 0; i < 4; i++) sb.append(digitColor).append(digits[i]);
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();
            } else if (level < 2000) {
                String numberColor, symbolColor;
                int decade = (level - 1200) / 100;
                switch (decade) {
                    case 0:
                        symbolColor = "§6";
                        numberColor = "§e";
                        break;
                    case 1:
                        symbolColor = "§3";
                        numberColor = "§b";
                        break;
                    case 2:
                        symbolColor = "§2";
                        numberColor = "§a";
                        break;
                    case 3:
                        symbolColor = "§9";
                        numberColor = "§3";
                        break;
                    case 4:
                        symbolColor = "§4";
                        numberColor = "§c";
                        break;
                    case 5:
                        symbolColor = "§5";
                        numberColor = "§d";
                        break;
                    case 6:
                        symbolColor = "§1";
                        numberColor = "§9";
                        break;
                    case 7:
                        symbolColor = "§0";
                        numberColor = "§5";
                        break;
                    default:
                        symbolColor = "§7";
                        numberColor = "§7";
                        break;
                }
                String bracketLeft = numberColor + "[";
                String bracketRight = numberColor + "]";
                String symbol = "✪";
                sb.append(bracketLeft);
                for (char digit : digits) sb.append(numberColor).append(digit);
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();
            } else {
                String bracketLeft = "§8[";
                String bracketRight = "§8]";
                String symbolColor = "§7";
                String symbol = "✪";
                String[] digitColors = {"§7", "§f", "§f", "§7"};
                sb.append(bracketLeft);
                for (int i = 0; i < 4; i++) sb.append(digitColors[i]).append(digits[i]);
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();
            }
        }

        String levelColor;
        if (level >= 900) levelColor = "§5";
        else if (level >= 800) levelColor = "§9";
        else if (level >= 700) levelColor = "§d";
        else if (level >= 600) levelColor = "§4";
        else if (level >= 500) levelColor = "§3";
        else if (level >= 400) levelColor = "§2";
        else if (level >= 300) levelColor = "§b";
        else if (level >= 200) levelColor = "§6";
        else if (level >= 100) levelColor = "§f";
        else levelColor = "§7";

        return levelColor + "[" + level + "✫" + levelColor + "]";
    }

    public static String getFormattedFinals(int finals) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        String color;
        if (finals >= 50000) color = "§5";
        else if (finals >= 20000) color = "§4";
        else if (finals >= 10000) color = "§c";
        else if (finals >= 5000) color = "§6";
        else if (finals >= 3000) color = "§e";
        else if (finals >= 1000) color = "§f";
        else color = "§7";
        return color + formatter.format(finals);
    }

    public static String getColoredFKDR(double fkdr) {
        DecimalFormat df = new DecimalFormat("#.##");
        String color;
        if (fkdr >= 30) color = "§5";
        else if (fkdr >= 15) color = "§4";
        else if (fkdr >= 8) color = "§c";
        else if (fkdr >= 4) color = "§6";
        else if (fkdr >= 3) color = "§e";
        else if (fkdr >= 1) color = "§f";
        else color = "§7";
        return color + df.format(fkdr);
    }
}