package com.konayuki.statflex.utils.chat;

import com.konayuki.statflex.utils.Settings;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.hypixel.Ranks;
import com.konayuki.statflex.features.bedwars.Bedwars;
import com.konayuki.statflex.features.bedwars.BedwarsList;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

public class Warn {

    public static void Warn(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        int warnLevel = Settings.getInstance().warnLevel;
        double warnFKDR = Settings.getInstance().warnFKDR;
        if (warnLevel <= 0 && warnFKDR <= 0) {
            return;
        }

        new Thread(() -> {
            try {
                HypixelApi.result result = HypixelApi.Fetch(playerName);
                if (!result.success) {
                    HypixelApi.sendError(result);
                    return;
                }

                JsonObject player = result.player;
                if (!player.has("stats") || !player.getAsJsonObject("stats").has("Bedwars")) {
                    return;
                }
                JsonObject stats = player.getAsJsonObject("stats").getAsJsonObject("Bedwars");

                int level = player.has("achievements") && player.getAsJsonObject("achievements").has("bedwars_level")
                        ? player.getAsJsonObject("achievements").get("bedwars_level").getAsInt()
                        : 0;
                int finals = stats.has("final_kills_bedwars") ? stats.get("final_kills_bedwars").getAsInt() : 0;
                int deaths = stats.has("final_deaths_bedwars") ? stats.get("final_deaths_bedwars").getAsInt() : 1;
                double fkdr = deaths == 0 ? finals : (double) finals / deaths;

                boolean matches = true;
                if (warnLevel > 0) {
                    matches &= level >= warnLevel;
                }
                if (warnFKDR > 0) {
                    matches &= fkdr >= warnFKDR;
                }
                if (!matches) {
                    return;
                }

                BedwarsList.PlayerData data = new BedwarsList.PlayerData(
                        Bedwars.getColoredLevel(level),
                        Ranks.getColoredPlayerName(player, result.properName),
                        Bedwars.getFormattedFinals(finals),
                        Bedwars.getColoredFKDR(fkdr),
                        level * fkdr,
                        level,
                        fkdr,
                        finals,
                        result.properName
                );
                sendWarning(buildStatWarning(data));
            } catch (Exception ignored) {
            }
        }, "Warn").start();
    }

    public static String buildStatWarning(BedwarsList.PlayerData data) {
        String starIcon = data.level <= 1099 ? "✫" : "✪";
        String plainLevel = Bedwars.formatLevelPlain(data.level);
        String plainFinals = Bedwars.formatFinalsPlain(data.finals);
        String plainFKDR = Bedwars.formatFKDRPlain(data.fkdr);
        return starIcon + plainLevel + " " + data.plainName + " | Finals: " + plainFinals + " | FKDR: " + plainFKDR;
    }

    public static void sendWarning(String warning) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc " + warning);
            }
        });

        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {
        }
    }
}
