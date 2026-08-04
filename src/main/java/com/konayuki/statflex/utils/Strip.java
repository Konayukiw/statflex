package com.konayuki.statflex.utils;

public final class Strip {
    private Strip() {
    }

    public static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("§.", "");
    }
}
