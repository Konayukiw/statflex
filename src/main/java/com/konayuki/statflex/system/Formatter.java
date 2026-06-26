package com.konayuki.statflex.system;

import com.google.gson.JsonObject;

public final class Formatter {
    private Formatter() {
    }

    public static String getColoredPlayerName(JsonObject player, String correctName) {
        String color = "§7";

        if (player.has("rank")
                && "YOUTUBER".equalsIgnoreCase(player.get("rank").getAsString())) {
            color = "§c";
        } else if (player.has("monthlyPackageRank")
                && "SUPERSTAR".equalsIgnoreCase(player.get("monthlyPackageRank").getAsString())) {
            color = "§6";
        } else if (player.has("newPackageRank") && !player.get("newPackageRank").isJsonNull()) {
            switch (player.get("newPackageRank").getAsString()) {
                case "VIP":
                case "VIP_PLUS":
                    color = "§a";
                    break;

                case "MVP":
                case "MVP_PLUS":
                    color = "§b";
                    break;
            }
        }

        return color + correctName;
    }
}
