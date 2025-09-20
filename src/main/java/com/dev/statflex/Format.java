package com.dev.statflex;

import com.google.gson.JsonObject;

public class Format {

    public static String getColoredPlayerName(JsonObject player, String correctName) {
        String color = "§7";

        if (player.has("monthlyPackageRank") &&
                player.get("monthlyPackageRank").getAsString().equalsIgnoreCase("SUPERSTAR")) {
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
                case "YOUTUBE":
                    color = "§c";
                default:
                    color = "§7";
            }
        }

        return color + correctName;
    }
}
