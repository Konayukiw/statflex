package com.dev.statflex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

public class SwFetcher {

    public static void fetchStats(String playerName, String mode) {
        new Thread(() -> {
            try {
                String apiKey = ApiKeyManager.getApiKey();
                if (apiKey.equals("N/A")) {
                    sendChat(Messages.API_INVALID);
                    return;
                }

                GetUUID.PlayerInfo info = GetUUID.getPlayerInfo(playerName);
                if (info == null) {
                    sendChat(Messages.PLAYER_NOT_FOUND);
                    return;
                }

                String uuid = info.uuid;
                String properName = info.name;

                String urlStr = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");

                JsonObject response = new JsonParser()
                        .parse(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))
                        .getAsJsonObject();

                if (!response.has("success") || !response.get("success").getAsBoolean()) {
                    String cause = response.has("cause") && !response.get("cause").isJsonNull()
                            ? response.get("cause").getAsString()
                            : "Unknown error";
                    sendChat(Messages.FETCH_ERROR + cause);
                    return;
                }

                JsonObject player = response.getAsJsonObject("player");
                if (player == null || player.isJsonNull()) {
                    sendChat(Messages.PLAYER_NOT_FOUND);
                    return;
                }

                JsonObject statsRoot = player.has("stats") && player.get("stats").isJsonObject()
                        ? player.getAsJsonObject("stats")
                        : null;
                if (statsRoot == null || !statsRoot.has("SkyWars") || !statsRoot.get("SkyWars").isJsonObject()) {
                    sendChat(Messages.FETCH_ERROR + " §b§lSky§e§lWars§7 stats not found");
                    return;
                }

                JsonObject stats = statsRoot.getAsJsonObject("SkyWars");

                String rawFormatted = stats.has("levelFormattedWithBrackets")
                        ? stats.get("levelFormattedWithBrackets").getAsString()
                        : "§7[N/A]";

                String levelFormatted = sanitizeFormattedLevel(rawFormatted);

                String modeKey = getModeKey(mode);
                String coloredPlayerName = Format.getColoredPlayerName(player, properName);

                String killsKey = "kills" + (modeKey != null ? "_" + modeKey : "");
                String winsKey = "wins" + (modeKey != null ? "_" + modeKey : "");
                String deathsKey = "deaths" + (modeKey != null ? "_" + modeKey : "");

                int wins = stats.has(winsKey) ? stats.get(winsKey).getAsInt() : 0;
                int kills = stats.has(killsKey) ? stats.get(killsKey).getAsInt() : 0;
                int deaths = stats.has(deathsKey) ? stats.get(deathsKey).getAsInt() : 1;
                double kdr = deaths == 0 ? kills : (double) kills / deaths;

                String formattedWins = getFormattedWins(wins);
                String formattedKDR = getColoredKDR(kdr);

                if (mode != null) {
                    String displayMode = getModeDisplayName(mode.toLowerCase());
                    if (displayMode == null)
                        displayMode = mode;

                    sendChat(String.format("§8[§cS§8] §b§lSky§e§lWars §7[§e%s§7]", displayMode));
                    sendChat(String.format("§c || %s %s §7| Wins: %s §7| KDR: %s",
                            levelFormatted, coloredPlayerName, formattedWins, formattedKDR));
                } else {
                    sendChat(String.format("§8[§cS§8] §b§lSky§e§lWars §7|"));
                    sendChat(String.format("§c || %s %s §7| Wins: %s §7| KDR: %s",
                            levelFormatted, coloredPlayerName, formattedWins, formattedKDR));
                }

            } catch (Exception e) {
                sendChat(Messages.FETCH_ERROR + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            }
        }).start();
    }

    private static String sanitizeFormattedLevel(String raw) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                result.append(c).append(raw.charAt(i + 1));
                i += 2;
                continue;
            }

            if (Character.isSurrogate(c)) {
                if (Character.isHighSurrogate(c) && (i + 1 < raw.length())
                        && Character.isLowSurrogate(raw.charAt(i + 1))) {
                    result.append(c).append(raw.charAt(i + 1));
                    i += 2;
                    continue;
                }
            }

            int type = Character.getType(c);
            if (Character.isLetterOrDigit(c) || c == '[' || c == ']' || c == '.' || c == '_' ||
                    type == Character.OTHER_SYMBOL ||
                    type == Character.MATH_SYMBOL ||
                    type == Character.LETTER_NUMBER ||
                    type == Character.OTHER_LETTER) {
                result.append(c);
            }
            i++;
        }
        return result.toString();
    }

    private static String getModeKey(String input) {
        if (input == null)
            return null;
        switch (input.toLowerCase()) {
            case "solo":
            case "1s":
                return "solo";
            case "doubles":
            case "duos":
            case "2s":
                return "team";
            case "mini":
                return "mini";
            default:
                return null;
        }
    }

    private static String getModeDisplayName(String input) {
        if (input == null)
            return null;
        switch (input.toLowerCase()) {
            case "solo":
            case "1s":
                return "Solo";
            case "doubles":
            case "duos":
            case "2s":
                return "Doubles";
            case "mini":
                return "Mini";
            default:
                return null;
        }
    }

    private static String getFormattedWins(int wins) {
        DecimalFormat formatter = new DecimalFormat("#,###");

        String color;
        if (wins >= 50000)
            color = "§5";
        else if (wins >= 20000)
            color = "§4";
        else if (wins >= 10000)
            color = "§c";
        else if (wins >= 5000)
            color = "§6";
        else if (wins >= 3000)
            color = "§e";
        else if (wins >= 1000)
            color = "§f";
        else
            color = "§7";

        return color + formatter.format(wins);
    }

    private static String getColoredKDR(double kdr) {
        DecimalFormat df = new DecimalFormat("#.##");

        String color;
        if (kdr >= 30)
            color = "§5";
        else if (kdr >= 15)
            color = "§4";
        else if (kdr >= 8)
            color = "§c";
        else if (kdr >= 4)
            color = "§6";
        else if (kdr >= 3)
            color = "§e";
        else if (kdr >= 1)
            color = "§f";
        else
            color = "§7";

        return color + df.format(kdr);
    }

    private static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }
}
