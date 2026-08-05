package com.konayuki.statflex.features.bedwars;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.hypixel.Ranks;
import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.utils.Messages;

import com.google.gson.JsonObject;

import net.minecraft.util.EnumChatFormatting;

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

                String line = String.format(Color.RED + " || %s %s " + Color.GRAY + "| Finals: %s "
                        + Color.GRAY + "| FKDR: %s", coloredLevel, coloredPlayerName, formattedFinals, coloredFKDR);

                if (mode != null && !mode.isEmpty()) {
                    String displayMode = getModeDisplayName(mode.toLowerCase());
                    if (displayMode == null) displayMode = mode;
                    Chat.send(String.format(Messages.BEDWARS_STATS + Color.GRAY + "[" + Color.YELLOW + "%s"
                            + Color.GRAY + "]", displayMode));
                } else {
                    Chat.send(Messages.BEDWARS_STATS);
                }
                Chat.send(line);

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
                EnumChatFormatting bracketLeft = Color.RED;
                EnumChatFormatting bracketRight = Color.DARK_PURPLE;
                EnumChatFormatting[] digitColors = {Color.GOLD, Color.YELLOW, Color.GREEN, Color.AQUA};
                EnumChatFormatting symbolColor = Color.LIGHT_PURPLE;
                String symbol = "✫";
                sb.append(bracketLeft).append("[");
                for (int i = 0; i < 4; i++) sb.append(digitColors[i]).append(digits[i]);
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight).append("]");
                return sb.toString();
            } else if (level < 1200) {
                EnumChatFormatting bracketColor = Color.GRAY;
                EnumChatFormatting digitColor = Color.WHITE;
                EnumChatFormatting symbolColor = Color.GRAY;
                String symbol = "✪";
                sb.append(bracketColor).append("[");
                for (int i = 0; i < 4; i++) sb.append(digitColor).append(digits[i]);
                sb.append(symbolColor).append(symbol);
                sb.append(bracketColor).append("]");
                return sb.toString();
            } else if (level < 2000) {
                EnumChatFormatting numberColor, symbolColor;
                int decade = (level - 1200) / 100;
                switch (decade) {
                    case 0:
                        symbolColor = Color.GOLD;
                        numberColor = Color.YELLOW;
                        break;
                    case 1:
                        symbolColor = Color.DARK_AQUA;
                        numberColor = Color.AQUA;
                        break;
                    case 2:
                        symbolColor = Color.DARK_GREEN;
                        numberColor = Color.GREEN;
                        break;
                    case 3:
                        symbolColor = Color.BLUE;
                        numberColor = Color.DARK_AQUA;
                        break;
                    case 4:
                        symbolColor = Color.DARK_RED;
                        numberColor = Color.RED;
                        break;
                    case 5:
                        symbolColor = Color.DARK_PURPLE;
                        numberColor = Color.LIGHT_PURPLE;
                        break;
                    case 6:
                        symbolColor = Color.DARK_BLUE;
                        numberColor = Color.BLUE;
                        break;
                    case 7:
                        symbolColor = Color.BLACK;
                        numberColor = Color.DARK_PURPLE;
                        break;
                    default:
                        symbolColor = Color.GRAY;
                        numberColor = Color.GRAY;
                        break;
                }
                String symbol = "✪";
                sb.append(numberColor).append("[");
                for (char digit : digits) sb.append(numberColor).append(digit);
                sb.append(symbolColor).append(symbol);
                sb.append(numberColor).append("]");
                return sb.toString();
            } else {
                EnumChatFormatting bracketColor = Color.DARK_GRAY;
                EnumChatFormatting symbolColor = Color.GRAY;
                String symbol = "✪";
                EnumChatFormatting[] digitColors = {Color.GRAY, Color.WHITE, Color.WHITE, Color.GRAY};
                sb.append(bracketColor).append("[");
                for (int i = 0; i < 4; i++) sb.append(digitColors[i]).append(digits[i]);
                sb.append(symbolColor).append(symbol);
                sb.append(bracketColor).append("]");
                return sb.toString();
            }
        }

        EnumChatFormatting levelColor;
        if (level >= 900) levelColor = Color.DARK_PURPLE;
        else if (level >= 800) levelColor = Color.BLUE;
        else if (level >= 700) levelColor = Color.LIGHT_PURPLE;
        else if (level >= 600) levelColor = Color.DARK_RED;
        else if (level >= 500) levelColor = Color.DARK_AQUA;
        else if (level >= 400) levelColor = Color.DARK_GREEN;
        else if (level >= 300) levelColor = Color.AQUA;
        else if (level >= 200) levelColor = Color.GOLD;
        else if (level >= 100) levelColor = Color.WHITE;
        else levelColor = Color.GRAY;

        return levelColor + "[" + level + "✫" + levelColor + "]";
    }

    public static String getFormattedFinals(int finals) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        EnumChatFormatting color;
        if (finals >= 50000) color = Color.DARK_PURPLE;
        else if (finals >= 20000) color = Color.DARK_RED;
        else if (finals >= 10000) color = Color.RED;
        else if (finals >= 5000) color = Color.GOLD;
        else if (finals >= 3000) color = Color.YELLOW;
        else if (finals >= 1000) color = Color.WHITE;
        else color = Color.GRAY;
        return color + formatter.format(finals);
    }

    public static String getColoredFKDR(double fkdr) {
        DecimalFormat df = new DecimalFormat("#.##");
        EnumChatFormatting color;
        if (fkdr >= 30) color = Color.DARK_PURPLE;
        else if (fkdr >= 15) color = Color.DARK_RED;
        else if (fkdr >= 8) color = Color.RED;
        else if (fkdr >= 4) color = Color.GOLD;
        else if (fkdr >= 3) color = Color.YELLOW;
        else if (fkdr >= 1) color = Color.WHITE;
        else color = Color.GRAY;
        return color + df.format(fkdr);
    }
}