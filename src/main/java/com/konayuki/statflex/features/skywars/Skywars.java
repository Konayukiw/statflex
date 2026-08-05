package com.konayuki.statflex.features.skywars;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.hypixel.Ranks;
import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.utils.Messages;

import com.google.gson.JsonObject;

import net.minecraft.util.EnumChatFormatting;

import java.text.DecimalFormat;

public class Skywars {

    public static void fetchStats(String playerName, String mode) {
        new Thread(() -> {
            try {
                HypixelApi.result result = HypixelApi.Fetch(playerName);
                if (!result.success) {
                    HypixelApi.sendError(result);
                    return;
                }

                JsonObject player = result.player;
                String properName = result.properName;

                JsonObject statsRoot = player.has("stats") && player.get("stats").isJsonObject()
                        ? player.getAsJsonObject("stats")
                        : null;
                if (statsRoot == null || !statsRoot.has("SkyWars") || !statsRoot.get("SkyWars").isJsonObject()) {
                    Chat.send(Messages.FETCH_ERROR + " No stats found for name" + playerName);
                    return;
                }

                JsonObject stats = statsRoot.getAsJsonObject("SkyWars");

                String rawFormatted = stats.has("levelFormattedWithBrackets")
                        ? stats.get("levelFormattedWithBrackets").getAsString()
                        : Color.GRAY + "[N/A]";

                String modeKey = getModeKey(mode);
                String coloredPlayerName = Ranks.getColoredPlayerName(player, properName);

                String killsKey = "kills" + (modeKey != null ? "_" + modeKey : "");
                String winsKey = "wins" + (modeKey != null ? "_" + modeKey : "");
                String deathsKey = "deaths" + (modeKey != null ? "_" + modeKey : "");

                int wins = stats.has(winsKey) ? stats.get(winsKey).getAsInt() : 0;
                int kills = stats.has(killsKey) ? stats.get(killsKey).getAsInt() : 0;
                int deaths = stats.has(deathsKey) ? stats.get(deathsKey).getAsInt() : 1;
                double kdr = deaths == 0 ? kills : (double) kills / deaths;

                String formattedWins = getFormattedWins(wins);
                String formattedKDR = getColoredKDR(kdr);

                String line = String.format(Color.RED + " || %s %s " + Color.GRAY + "| Wins: %s "
                        + Color.GRAY + "| KDR: %s", rawFormatted, coloredPlayerName, formattedWins, formattedKDR);

                if (mode != null) {
                    String displayMode = getModeDisplayName(mode.toLowerCase());
                    if (displayMode == null)
                        displayMode = mode;

                    Chat.send(String.format(Messages.SKYWARS_STATS + Color.GRAY + "[" + Color.YELLOW + "%s"
                            + Color.GRAY + "]", displayMode));
                } else {
                    Chat.send(Messages.SKYWARS_STATS);
                }
                Chat.send(line);

            } catch (Exception e) {
                Chat.send(Messages.FETCH_ERROR + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? ": " + e.getMessage() : ""));
            }
        }, "Skywars").start();
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

    public static String getFormattedWins(int wins) {
        DecimalFormat formatter = new DecimalFormat("#,###");

        EnumChatFormatting color;
        if (wins >= 50000)
            color = Color.DARK_PURPLE;
        else if (wins >= 20000)
            color = Color.DARK_RED;
        else if (wins >= 10000)
            color = Color.RED;
        else if (wins >= 5000)
            color = Color.GOLD;
        else if (wins >= 3000)
            color = Color.YELLOW;
        else if (wins >= 1000)
            color = Color.WHITE;
        else
            color = Color.GRAY;

        return color + formatter.format(wins);
    }

    public static String getColoredKDR(double kdr) {
        DecimalFormat df = new DecimalFormat("#.##");

        EnumChatFormatting color;
        if (kdr >= 30)
            color = Color.DARK_PURPLE;
        else if (kdr >= 15)
            color = Color.DARK_RED;
        else if (kdr >= 8)
            color = Color.RED;
        else if (kdr >= 4)
            color = Color.GOLD;
        else if (kdr >= 3)
            color = Color.YELLOW;
        else if (kdr >= 1)
            color = Color.WHITE;
        else
            color = Color.GRAY;

        return color + df.format(kdr);
    }
}
