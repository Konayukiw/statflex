package com.konayuki.statflex.utils;

import net.minecraft.util.EnumChatFormatting;

public final class Color {
    public static final EnumChatFormatting BLACK = EnumChatFormatting.BLACK;                 // §0
    public static final EnumChatFormatting DARK_BLUE = EnumChatFormatting.DARK_BLUE;         // §1
    public static final EnumChatFormatting DARK_GREEN = EnumChatFormatting.DARK_GREEN;       // §2
    public static final EnumChatFormatting DARK_AQUA = EnumChatFormatting.DARK_AQUA;         // §3
    public static final EnumChatFormatting DARK_RED = EnumChatFormatting.DARK_RED;           // §4
    public static final EnumChatFormatting DARK_PURPLE = EnumChatFormatting.DARK_PURPLE;     // §5
    public static final EnumChatFormatting GOLD = EnumChatFormatting.GOLD;                   // §6
    public static final EnumChatFormatting GRAY = EnumChatFormatting.GRAY;                   // §7
    public static final EnumChatFormatting DARK_GRAY = EnumChatFormatting.DARK_GRAY;         // §8
    public static final EnumChatFormatting BLUE = EnumChatFormatting.BLUE;                   // §9
    public static final EnumChatFormatting GREEN = EnumChatFormatting.GREEN;                 // §a
    public static final EnumChatFormatting AQUA = EnumChatFormatting.AQUA;                   // §b
    public static final EnumChatFormatting RED = EnumChatFormatting.RED;                     // §c
    public static final EnumChatFormatting LIGHT_PURPLE = EnumChatFormatting.LIGHT_PURPLE;   // §d
    public static final EnumChatFormatting YELLOW = EnumChatFormatting.YELLOW;               // §e
    public static final EnumChatFormatting WHITE = EnumChatFormatting.WHITE;                 // §f
    public static final EnumChatFormatting OBF = EnumChatFormatting.OBFUSCATED;              // §k
    public static final EnumChatFormatting BOLD = EnumChatFormatting.BOLD;                   // §l
    public static final EnumChatFormatting STR = EnumChatFormatting.STRIKETHROUGH;           // §m
    public static final EnumChatFormatting UNDERLINE = EnumChatFormatting.UNDERLINE;         // §n
    public static final EnumChatFormatting ITALIC = EnumChatFormatting.ITALIC;               // §o
    public static final EnumChatFormatting RESET = EnumChatFormatting.RESET;                 // §r

    private Color() {
    }

    public static String of(EnumChatFormatting... formats) {
        StringBuilder sb = new StringBuilder();
        for (EnumChatFormatting format : formats) {
            if (format != null) {
                sb.append(format);
            }
        }
        return sb.toString();
    }
}
