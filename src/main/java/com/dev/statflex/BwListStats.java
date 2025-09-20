package com.dev.statflex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BwListStats {

    private static final List<String> collectedPlayers = new ArrayList<>();
    private static final Pattern playerNamePattern = Pattern.compile("\\b[a-zA-Z0-9_]{3,16}\\b");

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Fetcher.isListStatsEnabled())
            return;
        String unformatted = event.message.getUnformattedText();

        if (unformatted.startsWith("ONLINE:")) {
            if (!Fetcher.isKeepWhoEnabled()) {
                event.setCanceled(true);
            }
            collectedPlayers.clear();
            extractPlayerNames(unformatted);
            sendChat("§8[§cS§8]§7 §c§lBed§f§lWars §7stats for current game |");
            listBedwarsStats(new ArrayList<>(collectedPlayers));
        }
    }

    private void extractPlayerNames(String text) {
        Matcher matcher = playerNamePattern.matcher(text);
        while (matcher.find()) {
            String playerName = matcher.group();
            if (playerName.equalsIgnoreCase("Online"))
                continue;
            if (!collectedPlayers.contains(playerName)) {
                collectedPlayers.add(playerName);
            }
        }
    }

    public static void listBedwarsStats(List<String> playerNames) {

        for (String name : playerNames) {
            new Thread(() -> {
                try {
                    String apiKey = ApiKeyManager.getApiKey();
                    if (apiKey.equals("N/A")) {
                        sendChat("§8[§cS§8]§7 API Key is not set.");
                        return;
                    }

                    GetUUID.PlayerInfo info = GetUUID.getPlayerInfo(name);
                    if (info == null) {
                        sendChat("§8[§cS§8]§7 Player not found: " + name);
                        return;
                    }

                    String uuid = info.uuid;
                    String properName = info.name;

                    String urlStr = "https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid;
                    HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                    connection.setRequestMethod("GET");

                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonElement element = new JsonParser().parse(reader);
                    JsonObject response = element.getAsJsonObject();

                    if (!response.has("success") || !response.get("success").getAsBoolean()) {
                        sendChat("§8[§cS§8]§7 Failed to fetch data for " + name);
                        return;
                    }

                    JsonObject player = response.getAsJsonObject("player");
                    JsonObject stats = player.has("stats") && player.getAsJsonObject("stats").has("Bedwars")
                            ? player.getAsJsonObject("stats").getAsJsonObject("Bedwars")
                            : new JsonObject();

                    int level = player.has("achievements")
                            && player.getAsJsonObject("achievements").has("bedwars_level")
                                    ? player.getAsJsonObject("achievements").get("bedwars_level").getAsInt()
                                    : 0;

                    int finals = stats.has("final_kills_bedwars") ? stats.get("final_kills_bedwars").getAsInt() : 0;
                    int deaths = stats.has("final_deaths_bedwars") ? stats.get("final_deaths_bedwars").getAsInt() : 1;
                    double fkdr = deaths == 0 ? finals : (double) finals / deaths;

                    String coloredLevel = BwFetcher.getColoredLevel(level);
                    String coloredPlayerName = Format.getColoredPlayerName(player, properName);
                    String formattedFinals = BwFetcher.getFormattedFinals(finals);
                    String coloredFKDR = BwFetcher.getColoredFKDR(fkdr);

                    String msg = String.format("%s %s §7| Finals: %s §7| FKDR: %s",
                            coloredLevel, coloredPlayerName, formattedFinals, coloredFKDR);
                    sendChat(msg);

                } catch (Exception e) {
                    sendChat("§8[§cS§8]§7 Error getting stats for " + name);
                }
            }).start();
        }
    }

    private static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }
}
