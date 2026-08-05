package com.konayuki.statflex.utils.hypixel;

import com.google.gson.JsonObject;
import com.konayuki.statflex.utils.Color;

import net.minecraft.util.EnumChatFormatting;

public final class Ranks {
    private Ranks() {
    }

    public static String rank(JsonObject player, String correctName) {
        EnumChatFormatting color = Color.GRAY;

        if (player.has("rank")
                && "YOUTUBER".equalsIgnoreCase(player.get("rank").getAsString())) {
            color = Color.RED;
        } else if (player.has("monthlyPackageRank")
                && "SUPERSTAR".equalsIgnoreCase(player.get("monthlyPackageRank").getAsString())) {
            color = Color.GOLD;
        } else if (player.has("newPackageRank") && !player.get("newPackageRank").isJsonNull()) {
            switch (player.get("newPackageRank").getAsString()) {
                case "VIP":
                case "VIP_PLUS":
                    color = Color.GREEN;
                    break;

                case "MVP":
                case "MVP_PLUS":
                    color = Color.AQUA;
                    break;
            }
        }

        return color + correctName;
    }
}
