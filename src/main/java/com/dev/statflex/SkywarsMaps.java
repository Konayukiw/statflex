package com.dev.statflex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SkywarsMaps {

    public static final Set<String> MAPS = new HashSet<>(Arrays.asList(
            "Aegis", "Aelle", "Ancient", "Aquacrown", "Aquarius", "Arkrose", "Arule", "Atlas", "Atuin",
            "Bonsai", "Canopy", "Causeway", "Checkered Manor", "Chronos", "Citadel", "Clearing", "Crumble",
            "Crystal Source", "Dawn", "Deadland", "Deserted Dunes", "Desserted Island", "Dwarven", "Elven",
            "Embercell", "Entangled", "Felkenheart", "Firelink Shrine", "Forest", "Forgotten", "Fossil",
            "Fragment", "Frostbound", "Garage", "Garden Bed", "Garrison", "Glacier", "Hanging Gardens",
            "Hanabira", "Harmony", "Heaven Palace", "Helios", "Hibiscus", "Honeycomb", "Jagged", "Jinzhou",
            "Kiryu", "Long Island", "Martian", "Memorial", "Metari Temple", "Mont Golball", "Mothership",
            "Mountain Top", "Mushy", "Mythic", "Mythos", "Nian", "Niu", "Nomad", "Oberon Towers", "Oceana",
            "Onionring", "Onionring 2", "Overfall", "Palette", "Plateau", "Radis", "Railroad", "Railwork",
            "Redfang", "Roots", "Sanctuary", "Sanctum", "Sentinel", "Shaohao", "Shire", "Siege", "Simiao",
            "Skychurch", "Steampunk", "Strata", "Stronghold", "Submerged", "Tain", "Teatime", "Toadstool",
            "Towers", "Tribal", "Tribute", "Tundra", "Ukishima", "Villa", "Waititi", "Waterways", "Winterhelm",
            "Workshop", "Basket", "Beetle", "Blossom", "Cacti", "Egg Isle", "Farmstead", "Mushroom Vale",
            "Pralines", "Rustic", "Shoompa", "Aku", "Craboab", "Kingdom", "Kraken", "Lake Attack", "Paradise",
            "Pit Stop", "Sunken", "Chateau", "Nightmare", "Undead Isle", "Witch's Brew", "Aurora",
            "Candylane", "Congestion", "Cookie Fest", "Frosty", "Frozen Shire", "Fruit Cake", "Glazed",
            "Sugar Rush", "Wrapping", "Wreath"));

    public static boolean containsSkywarsMap(String line) {
        String cleanLine = normalize(line);

        if (cleanLine.startsWith("map:")) {
            cleanLine = cleanLine.substring(4).trim();
        }

        for (String map : MAPS) {
            if (cleanLine.contains(normalize(map))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String input) {
        if (input == null)
            return "";
        return input
                .toLowerCase() // 小文字化
                .replaceAll("§.", "")
                .replaceAll("\\p{Z}", " ")
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }

}
