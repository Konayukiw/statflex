package com.dev.statflex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DuelsFetcherForUpdate {

    private static final Map<String, String[]> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put("prefix_scheme_boilerplate_gold", new String[] { "§6" });
        COLOR_MAP.put("prefix_scheme_absorption", new String[] { "§e" });
        COLOR_MAP.put("prefix_scheme_heavy_dark_green", new String[] { "§2" });
        COLOR_MAP.put("prefix_scheme_explosive_dark_red", new String[] { "§4" });
        COLOR_MAP.put("prefix_scheme_good_ol_gray", new String[] { "§7" });
        COLOR_MAP.put("prefix_scheme_vanilla_white", new String[] { "§f" });
        COLOR_MAP.put("prefix_scheme_ender_green", new String[] { "§a" });
        COLOR_MAP.put("prefix_scheme_platformer_dark_purple", new String[] { "§5" });
        COLOR_MAP.put("prefix_scheme_armored_aqua", new String[] { "§b" });
        COLOR_MAP.put("prefix_scheme_pearl_dark_aqua", new String[] { "§3" });
        COLOR_MAP.put("prefix_scheme_withering_back", new String[] { "§0" });
        COLOR_MAP.put("prefix_scheme_persistent_dark_blue", new String[] { "§1" });
        COLOR_MAP.put("prefix_scheme_carpeted_light_purple", new String[] { "§d" });
        COLOR_MAP.put("prefix_scheme_punchable_red", new String[] { "§c" });
        COLOR_MAP.put("prefix_scheme_drawstring_dark_gray", new String[] { "§8" });
        COLOR_MAP.put("prefix_scheme_blitz_blue", new String[] { "§9" });
        COLOR_MAP.put("prefix_scheme_blossoms", new String[] { "§5", "§d", "§f", "§d", "§f" });
        COLOR_MAP.put("prefix_scheme_ultra_hardcore_undertone", new String[] { "§2", "§a", "§e", "§e", "§6" });
        COLOR_MAP.put("prefix_scheme_in_case_of_chroma", new String[] { "§5", "§f", "§9", "§1" });
        COLOR_MAP.put("prefix_scheme_elusive_jeremiah_huestring", new String[] { "§3", "§6", "§6", "§e", "§b" });
        COLOR_MAP.put("prefix_scheme_healthy_stain", new String[] { "§8", "§7", "§7", "§f", "§c" });
        COLOR_MAP.put("prefix_scheme_four_team_tie_dye", new String[] { "§e", "§a", "§f", "§c", "§9" });
        COLOR_MAP.put("prefix_scheme_combo_coating", new String[] { "§4", "§c", "§6", "§e", "§f" });
        COLOR_MAP.put("prefix_scheme_mythic_pigment", new String[] { "§c", "§e", "§a", "§b", "§d" });
        COLOR_MAP.put("prefix_scheme_picturesque_firework", new String[] { "§1", "§9", "§f", "§c", "§4" });
        COLOR_MAP.put("prefix_scheme_punching_paint", new String[] { "§4", "§5", "§5", "§d", "§b" });
        COLOR_MAP.put("prefix_scheme_flint_gradient", new String[] { "§f", "§7", "§8", "§0", "§0" });
        COLOR_MAP.put("prefix_scheme_a_splash_of_star", new String[] { "§1", "§3", "§3", "§b" });
        COLOR_MAP.put("prefix_scheme_sunny_shades", new String[] { "§3", "§b", "§b", "§f", "§e" });
        COLOR_MAP.put("prefix_scheme_festive_finish", new String[] { "§2", "§c", "§c", "§c", "§2" });
        COLOR_MAP.put("prefix_scheme_with_a_side_of_skies", new String[] { "§3", "§a", "§a", "§a", "§2" });
        COLOR_MAP.put("prefix_scheme_overpowered_gloss", new String[] { "§d", "§d", "§b", "§b", "§f" });
        COLOR_MAP.put("prefix_scheme_the_impossible_varnish", new String[] { "§d", "§a", "§a", "§a", "§f" });
        COLOR_MAP.put("prefix_scheme_color_of_a_flash", new String[] { "§8", "§f", "§f", "§f", "§8" });
        COLOR_MAP.put("prefix_scheme_bedding_hues", new String[] { "§c", "§c", "§c", "§f", "§f" });
        COLOR_MAP.put("prefix_scheme_variety_values", new String[] { "§b", "§f", "§f", "§f", "§b" });
    }

    public static void fetchStats(String inputName, String mode) {
        fetchStats(inputName, mode, false);
    }

    public static void fetchStats(String inputName, String mode, boolean auto) {
        new Thread(() -> {
            try {
                String apiKey = ApiKeyManager.getApiKey();
                if (apiKey.equals("N/A")) {
                    sendChat("§8[§cS§8]§7 API Key is not set.");
                    return;
                }

                GetUUID.PlayerInfo info = GetUUID.getPlayerInfo(inputName);
                if (info == null) {
                    System.out.println("[S] Failed to fetch player stats: " + inputName);
                    return;
                }

                String uuid = info.uuid;
                String properName = info.name;

                String urlStr = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
                HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setRequestMethod("GET");

                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonParser parser = new JsonParser();
                JsonObject response = parser.parse(reader).getAsJsonObject();

                if (!response.has("success") || !response.get("success").getAsBoolean()) {
                    String cause = response.has("cause") && !response.get("cause").isJsonNull()
                            ? response.get("cause").getAsString()
                            : "Unknown error";
                    sendChat("§8[§cS§8]§7 Failed to fetch player data: " + cause);
                    return;
                }

                JsonElement playerElement = response.get("player");
                if (playerElement == null || playerElement.isJsonNull()) {
                    System.out.println("[S] Failed to fetch player stats: " + inputName);
                    return;
                }

                JsonObject player = playerElement.getAsJsonObject();
                JsonObject stats = player.has("stats") && player.getAsJsonObject("stats").has("Duels")
                        ? player.getAsJsonObject("stats").getAsJsonObject("Duels")
                        : new JsonObject();

                int wins, losses;
                double wlr;

                if (mode != null) {
                    String key = DuelsFetcher.getModeKey(mode);
                    if (key == null) {
                        sendChat("§8[§cS§8]§7 Invalid mode: " + mode);
                        return;
                    }
                    wins = stats.has(key + "_wins") ? stats.get(key + "_wins").getAsInt() : 0;
                    losses = stats.has(key + "_losses") ? stats.get(key + "_losses").getAsInt() : 1;
                } else {
                    wins = stats.has("wins") ? stats.get("wins").getAsInt() : 0;
                    losses = stats.has("losses") ? stats.get("losses").getAsInt() : 1;
                }
                wlr = losses == 0 ? wins : (double) wins / losses;

                String coloredPlayerName = Format.getColoredPlayerName(player, properName);
                String formattedWins = DuelsFetcher.getFormattedWins(wins);
                String coloredWLR = DuelsFetcher.getColoredWLR(wlr);

                String schemeName = null;
                if (stats.has("active_prefix_scheme") && !stats.get("active_prefix_scheme").isJsonNull()) {
                    schemeName = stats.get("active_prefix_scheme").getAsString();
                }

                String[] schemeColors = null;
                if (schemeName != null && COLOR_MAP.containsKey(schemeName)) {
                    schemeColors = COLOR_MAP.get(schemeName);

                    if (wins > 100_000) {
                        for (int i = 0; i < schemeColors.length; i++) {
                            if (schemeColors[i].startsWith("§") && schemeColors[i].length() >= 2) {
                                schemeColors[i] = schemeColors[i].substring(0, 2) + "§l" + schemeColors[i].substring(2);
                            }
                        }
                    }
                }

                String rawTitle = mode != null ? DuelsFetcher.getColoredTitle(wins, false)
                        : DuelsFetcher.getColoredTitle(wins, true);
                String plainTitle = stripColor(rawTitle);
                String modeDisplay = mode != null ? DuelsFetcher.getModeDisplayName(mode.toLowerCase()) : "";
                String plainMode = modeDisplay == null ? "" : stripColor(modeDisplay);

                String titleWithScheme = plainTitle;
                String modeWithScheme = plainMode;
                if (schemeColors != null) {
                    titleWithScheme = applyColorGradient(plainTitle, schemeColors);
                    modeWithScheme = applyColorGradient(plainMode, schemeColors);
                }

                String modeAndTitle = (modeWithScheme.isEmpty() ? "" : modeWithScheme + " ") + titleWithScheme;

                if (auto) {
                    sendChat(String.format("§8[§cS§8]§6 %s %s §7| Wins: %s §7| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                } else {
                    if (mode == null) {
                        sendChat("§8[§cS§8] §b§lDuels §7|");
                    } else {
                        sendChat(String.format("§8[§cS§8] §b§lDuels §7[§e%s§7]", modeDisplay));
                    }
                    sendChat(String.format("§c ||§6 %s %s §7| Wins: %s §7| WLR: %s",
                            modeAndTitle, coloredPlayerName, formattedWins, coloredWLR));
                }

            } catch (Exception e) {
                sendChat("§8[§cS§8]§7 Failed to fetch player stats: " + e.getClass().getSimpleName());
            }
        }).start();
    }

    public static String stripColor(String input) {
        if (input == null)
            return "";
        return input.replaceAll("§.", "");
    }

    private static String applyColorGradient(String text, String[] colors) {
        StringBuilder sb = new StringBuilder();
        int colorCount = colors.length;
        if (colorCount == 1) {
            sb.append(colors[0]).append(text);
            return sb.toString();
        }
        for (int i = 0; i < text.length(); i++) {
            String color = colors[Math.min(i, colorCount - 1)];
            sb.append(color).append(text.charAt(i));
        }
        return sb.toString();
    }

    public static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }
}