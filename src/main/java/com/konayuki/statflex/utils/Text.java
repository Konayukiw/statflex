package com.konayuki.statflex.utils;

public final class Text {
    private Text() {
    }

    public static String strip(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("§.", "");
    }
}
