package com.konayuki.statflex.features.duels;

import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Text;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.hypixel.Ranks;

import com.google.gson.JsonObject;

import net.minecraft.util.EnumChatFormatting;

import java.util.HashMap;
import java.util.Map;

public class DuelsUpdated {

    private static final Map<String, EnumChatFormatting[]> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put("prefix_scheme_boilerplate_gold", new EnumChatFormatting[] { Color.GOLD });
        COLOR_MAP.put("prefix_scheme_absorption", new EnumChatFormatting[] { Color.YELLOW });
        COLOR_MAP.put("prefix_scheme_heavy_dark_green", new EnumChatFormatting[] { Color.DARK_GREEN });
        COLOR_MAP.put("prefix_scheme_explosive_dark_red", new EnumChatFormatting[] { Color.DARK_RED });
        COLOR_MAP.put("prefix_scheme_good_ol_gray", new EnumChatFormatting[] { Color.GRAY });
        COLOR_MAP.put("prefix_scheme_vanilla_white", new EnumChatFormatting[] { Color.WHITE });
        COLOR_MAP.put("prefix_scheme_ender_green", new EnumChatFormatting[] { Color.GREEN });
        COLOR_MAP.put("prefix_scheme_platformer_dark_purple", new EnumChatFormatting[] { Color.DARK_PURPLE });
        COLOR_MAP.put("prefix_scheme_armored_aqua", new EnumChatFormatting[] { Color.AQUA });
        COLOR_MAP.put("prefix_scheme_pearl_dark_aqua", new EnumChatFormatting[] { Color.DARK_AQUA });
        COLOR_MAP.put("prefix_scheme_withering_back", new EnumChatFormatting[] { Color.BLACK });
        COLOR_MAP.put("prefix_scheme_persistent_dark_blue", new EnumChatFormatting[] { Color.DARK_BLUE });
        COLOR_MAP.put("prefix_scheme_carpeted_light_purple", new EnumChatFormatting[] { Color.LIGHT_PURPLE });
        COLOR_MAP.put("prefix_scheme_punchable_red", new EnumChatFormatting[] { Color.RED });
        COLOR_MAP.put("prefix_scheme_drawstring_dark_gray", new EnumChatFormatting[] { Color.DARK_GRAY });
        COLOR_MAP.put("prefix_scheme_blitz_blue", new EnumChatFormatting[] { Color.BLUE });

        COLOR_MAP.put("prefix_scheme_blossoms", new EnumChatFormatting[] {
                Color.DARK_PURPLE, Color.LIGHT_PURPLE, Color.WHITE, Color.LIGHT_PURPLE, Color.WHITE });
        COLOR_MAP.put("prefix_scheme_ultra_hardcore_undertone", new EnumChatFormatting[] {
                Color.DARK_GREEN, Color.GREEN, Color.YELLOW, Color.YELLOW, Color.GOLD });
        COLOR_MAP.put("prefix_scheme_in_case_of_chroma", new EnumChatFormatting[] {
                Color.DARK_PURPLE, Color.WHITE, Color.BLUE, Color.DARK_BLUE });
        COLOR_MAP.put("prefix_scheme_elusive_jeremiah_huestring", new EnumChatFormatting[] {
                Color.DARK_AQUA, Color.GOLD, Color.GOLD, Color.YELLOW, Color.AQUA });
        COLOR_MAP.put("prefix_scheme_healthy_stain", new EnumChatFormatting[] {
                Color.DARK_GRAY, Color.GRAY, Color.GRAY, Color.WHITE, Color.RED });
        COLOR_MAP.put("prefix_scheme_four_team_tie_dye", new EnumChatFormatting[] {
                Color.YELLOW, Color.GREEN, Color.WHITE, Color.RED, Color.BLUE });
        COLOR_MAP.put("prefix_scheme_combo_coating", new EnumChatFormatting[] {
                Color.DARK_RED, Color.RED, Color.GOLD, Color.YELLOW, Color.WHITE });
        COLOR_MAP.put("prefix_scheme_mythic_pigment", new EnumChatFormatting[] {
                Color.RED, Color.YELLOW, Color.GREEN, Color.AQUA, Color.LIGHT_PURPLE });
        COLOR_MAP.put("prefix_scheme_picturesque_firework", new EnumChatFormatting[] {
                Color.DARK_BLUE, Color.BLUE, Color.WHITE, Color.RED, Color.DARK_RED });
        COLOR_MAP.put("prefix_scheme_punching_paint", new EnumChatFormatting[] {
                Color.DARK_RED, Color.DARK_PURPLE, Color.DARK_PURPLE, Color.LIGHT_PURPLE, Color.AQUA });
        COLOR_MAP.put("prefix_scheme_flint_gradient", new EnumChatFormatting[] {
                Color.WHITE, Color.GRAY, Color.DARK_GRAY, Color.BLACK, Color.BLACK });
        COLOR_MAP.put("prefix_scheme_a_splash_of_star", new EnumChatFormatting[] {
                Color.DARK_BLUE, Color.DARK_AQUA, Color.DARK_AQUA, Color.AQUA });
        COLOR_MAP.put("prefix_scheme_sunny_shades", new EnumChatFormatting[] {
                Color.DARK_AQUA, Color.AQUA, Color.AQUA, Color.WHITE, Color.YELLOW });
        COLOR_MAP.put("prefix_scheme_festive_finish", new EnumChatFormatting[] {
                Color.DARK_GREEN, Color.RED, Color.RED, Color.RED, Color.DARK_GREEN });
        COLOR_MAP.put("prefix_scheme_with_a_side_of_skies", new EnumChatFormatting[] {
                Color.DARK_AQUA, Color.GREEN, Color.GREEN, Color.GREEN, Color.DARK_GREEN });
        COLOR_MAP.put("prefix_scheme_overpowered_gloss", new EnumChatFormatting[] {
                Color.LIGHT_PURPLE, Color.LIGHT_PURPLE, Color.AQUA, Color.AQUA, Color.WHITE });
        COLOR_MAP.put("prefix_scheme_the_impossible_varnish", new EnumChatFormatting[] {
                Color.LIGHT_PURPLE, Color.GREEN, Color.GREEN, Color.GREEN, Color.WHITE });
        COLOR_MAP.put("prefix_scheme_color_of_a_flash", new EnumChatFormatting[] {
                Color.DARK_GRAY, Color.WHITE, Color.WHITE, Color.WHITE, Color.DARK_GRAY });
        COLOR_MAP.put("prefix_scheme_bedding_hues", new EnumChatFormatting[] {
                Color.RED, Color.RED, Color.RED, Color.WHITE, Color.WHITE });
        COLOR_MAP.put("prefix_scheme_variety_values", new EnumChatFormatting[] {
                Color.AQUA, Color.WHITE, Color.WHITE, Color.WHITE, Color.AQUA });
    }

    public static void fetchStats(String inputName, String mode) {
        fetchStats(inputName, mode, false);
    }

    public static void fetchStats(String inputName, String mode, boolean auto) {
        new Thread(() -> {
            try {
                HypixelApi.result result = HypixelApi.fetch(inputName);
                if (!result.success) {
                    HypixelApi.error(result);
                    return;
                }

                JsonObject player = result.player;
                String properName = result.properName;
                JsonObject stats = player.has("stats") && player.getAsJsonObject("stats").has("Duels")
                        ? player.getAsJsonObject("stats").getAsJsonObject("Duels")
                        : new JsonObject();

                int wins, losses;
                double wlr;

                if (mode != null) {
                    String key = Duels.getModeKey(mode);
                    if (key == null) {
                        Chat.send(Messages.INVALID_MODE + mode);
                        return;
                    }
                    wins = stats.has(key + "_wins") ? stats.get(key + "_wins").getAsInt() : 0;
                    losses = stats.has(key + "_losses") ? stats.get(key + "_losses").getAsInt() : 1;
                } else {
                    wins = stats.has("wins") ? stats.get("wins").getAsInt() : 0;
                    losses = stats.has("losses") ? stats.get("losses").getAsInt() : 1;
                }
                wlr = losses == 0 ? wins : (double) wins / losses;

                String coloredPlayerName = Ranks.rank(player, properName);
                String formattedWins = Duels.getFormattedWins(wins);
                String coloredWLR = Duels.getColoredWLR(wlr);

                String schemeName = null;
                if (stats.has("active_prefix_scheme") && !stats.get("active_prefix_scheme").isJsonNull()) {
                    schemeName = stats.get("active_prefix_scheme").getAsString();
                }

                EnumChatFormatting[] schemeColors =
                        schemeName != null ? COLOR_MAP.get(schemeName) : null;
                boolean boldScheme = wins > 100_000;

                String rawTitle = mode != null ? Duels.getColoredTitle(wins, false)
                        : Duels.getColoredTitle(wins, true);
                String plainTitle = Text.strip(rawTitle);
                String modeDisplay = mode != null ? Duels.getModeDisplayName(mode.toLowerCase()) : "";
                String plainMode = modeDisplay == null ? "" : Text.strip(modeDisplay);

                String titleWithScheme = plainTitle;
                String modeWithScheme = plainMode;
                if (schemeColors != null) {
                    titleWithScheme = applyColorGradient(plainTitle, schemeColors, boldScheme);
                    modeWithScheme = applyColorGradient(plainMode, schemeColors, boldScheme);
                }

                String modeAndTitle = (modeWithScheme.isEmpty() ? "" : modeWithScheme + " ") + titleWithScheme;

                if (auto) {
                    Chat.send(String.format(Messages.PREFIX + Color.GOLD + " %s %s " + Color.GRAY + "| Wins: %s " + Color.GRAY + "| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                } else {
                    if (mode == null) {
                        Chat.send(Messages.DUELS_STATS);
                    } else {
                        Chat.send(String.format(Messages.DUELS_STATS + Color.GRAY + "[" + Color.YELLOW + "%s"
                                + Color.GRAY + "]", modeDisplay));
                    }
                    Chat.send(String.format(Color.RED + " ||" + Color.GOLD + " %s %s " + Color.GRAY
                                    + "| Wins: %s " + Color.GRAY + "| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                }

            } catch (Exception e) {
                Chat.send(Messages.FETCH_ERROR + e.getClass().getSimpleName());
            }
        }, "DuelsUpdated").start();
    }

    private static String applyColorGradient(String text, EnumChatFormatting[] colors, boolean bold) {
        StringBuilder sb = new StringBuilder();
        int colorCount = colors.length;
        if (colorCount == 1) {
            sb.append(colors[0]);
            if (bold) {
                sb.append(Color.BOLD);
            }
            sb.append(text);
            return sb.toString();
        }
        for (int i = 0; i < text.length(); i++) {
            sb.append(colors[Math.min(i, colorCount - 1)]);
            if (bold) {
                sb.append(Color.BOLD);
            }
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }
}