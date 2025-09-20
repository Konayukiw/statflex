package com.dev.statflex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class BwFetcher {

    public static void fetchStats(String inputName, String mode) {
        new Thread(() -> {
            try {
                String apiKey = ApiKeyManager.getApiKey();
                if (apiKey.equals("N/A")) {
                    sendChat("§8[§cS§8]§7 API Key is not set.");
                    return;
                }

                GetUUID.PlayerInfo info = GetUUID.getPlayerInfo(inputName);
                if (info == null) {
                    sendChat("§8[§cS§8]§7 Player not found for name " + inputName);
                    return;
                }

                String uuid = info.uuid;
                String properName = info.name;
                String urlStr = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonParser parser = new JsonParser();
                JsonObject response = parser.parse(reader).getAsJsonObject();

                if (!response.has("success") || !response.get("success").getAsBoolean()) {
                    String cause = response.has("cause") && !response.get("cause").isJsonNull()
                            ? response.get("cause").getAsString()
                            : "Unknown error";

                    if (cause.toLowerCase().contains("invalid") || cause.toLowerCase().contains("api key")) {
                        sendChat("§8[§cS§8]§7 Invalid API Key. Please set new API Key by /s api [API Key]. ");
                    } else {
                        sendChat("§8[§cS§8]§7 Failed to fetch player data: " + cause);
                    }
                    return;
                }
                JsonElement playerElement = response.get("player");
                if (playerElement == null || playerElement.isJsonNull()) {
                    sendChat("§8[§cS§8]§7 Player not found.");
                    return;
                }

                JsonObject player = playerElement.getAsJsonObject();
                JsonObject stats = player.has("stats") && player.getAsJsonObject("stats").has("Bedwars")
                        ? player.getAsJsonObject("stats").getAsJsonObject("Bedwars")
                        : new JsonObject();

                int level = player.has("achievements") && player.getAsJsonObject("achievements").has("bedwars_level")
                        ? player.getAsJsonObject("achievements").get("bedwars_level").getAsInt()
                        : 0;

                int finals, deaths;
                double fkdr;

                if (mode != null) {
                    String key = getModeKey(mode);
                    if (key == null) {
                        sendChat("§8[§cS§8]§7 Invalid mode: " + mode);
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
                String coloredPlayerName = Format.getColoredPlayerName(player, properName);
                String formattedFinals = getFormattedFinals(finals);
                String coloredFKDR = getColoredFKDR(fkdr);

                if (mode != null) {
                    String displayMode = getModeDisplayName(mode.toLowerCase());
                    if (displayMode == null)
                        displayMode = mode;

                    sendChat(String.format("§8[§cS§8] §c§lBed§f§lWars §7[§e%s§7]", displayMode));
                    sendChat(String.format("§c || %s %s §7| Finals: %s §7| FKDR: %s",
                            coloredLevel, coloredPlayerName, formattedFinals, coloredFKDR));
                } else {
                    sendChat(String.format("§8[§cS§8] §c§lBed§f§lWars §7|"));
                    sendChat(String.format("§c || %s %s §7| Finals: %s §7| FKDR: %s",
                            coloredLevel, coloredPlayerName, formattedFinals, coloredFKDR));
                }

            } catch (Exception e) {
                sendChat("§8[§cS§8]§7 Failed to fetch player stats: " + e.getClass().getSimpleName());
            }
        }).start();
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
                // 1000 - 1099
                String bracketLeft = "§c[";
                String bracketRight = "§5]";
                String[] digitColors = { "§6", "§e", "§a", "§b" };
                String symbolColor = "§d";
                String symbol = "✫";

                sb.append(bracketLeft);
                for (int i = 0; i < 4; i++) {
                    sb.append(digitColors[i]).append(digits[i]);
                }
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();

            } else if (level < 1200) {
                // 1100 - 1199
                String bracketLeft = "§7[";
                String bracketRight = "§7]";
                String digitColor = "§f";
                String symbolColor = "§7";
                String symbol = "✪";

                sb.append(bracketLeft);
                for (int i = 0; i < 4; i++) {
                    sb.append(digitColor).append(digits[i]);
                }
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();

            } else if (level < 2000) {
                // 1200 - 1999
                String numberColor;
                String symbolColor;

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
                for (char digit : digits) {
                    sb.append(numberColor).append(digit);
                }
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();

            } else {
                // 2000+
                String bracketLeft = "§8[";
                String bracketRight = "§8]";
                String symbolColor = "§7";
                String symbol = "✪";
                String[] digitColors = { "§7", "§f", "§f", "§7" }; // 1000, 100, 10, 1

                sb.append(bracketLeft);
                for (int i = 0; i < 4; i++) {
                    sb.append(digitColors[i]).append(digits[i]);
                }
                sb.append(symbolColor).append(symbol);
                sb.append(bracketRight);
                return sb.toString();
            }
        }

        // 0 - 999
        String levelColor;
        if (level >= 900)
            levelColor = "§5";
        else if (level >= 800)
            levelColor = "§9";
        else if (level >= 700)
            levelColor = "§d";
        else if (level >= 600)
            levelColor = "§4";
        else if (level >= 500)
            levelColor = "§3";
        else if (level >= 400)
            levelColor = "§2";
        else if (level >= 300)
            levelColor = "§b";
        else if (level >= 200)
            levelColor = "§6";
        else if (level >= 100)
            levelColor = "§f";
        else
            levelColor = "§7";

        return levelColor + "[" + level + "✫" + levelColor + "]";
    }

    public static String getFormattedFinals(int finals) {
        DecimalFormat formatter = new DecimalFormat("#,###");

        String color;
        if (finals >= 50000)
            color = "§5";
        else if (finals >= 20000)
            color = "§4";
        else if (finals >= 10000)
            color = "§c";
        else if (finals >= 5000)
            color = "§6";
        else if (finals >= 3000)
            color = "§e";
        else if (finals >= 1000)
            color = "§f";
        else
            color = "§7";

        return color + formatter.format(finals);
    }

    public static String getColoredFKDR(double fkdr) {
        DecimalFormat df = new DecimalFormat("#.##");

        String color;
        if (fkdr >= 30)
            color = "§5";
        else if (fkdr >= 15)
            color = "§4";
        else if (fkdr >= 8)
            color = "§c";
        else if (fkdr >= 4)
            color = "§6";
        else if (fkdr >= 3)
            color = "§e";
        else if (fkdr >= 1)
            color = "§f";
        else
            color = "§7";

        return color + df.format(fkdr);
    }

    private static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }
}
