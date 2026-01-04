package com.konayuki.statflex;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
        if (playerNames.isEmpty()) {
            return;
        }

        List<PlayerData> playerDatas = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(playerNames.size());

        for (String name : playerNames) {
            new Thread(() -> {
                try {
                    String apiKey = ApiKeyManager.getApiKey();
                    if (apiKey.equals("N/A")) {
                        sendChat("§8[§cS§8]§7 API Key is not set.");
                        latch.countDown();
                        return;
                    }

                    GetUUID.PlayerInfo info = GetUUID.getPlayerInfo(name);
                    if (info == null) {
                        sendChat("§8[§cS§8]§7 Player not found: " + name);
                        latch.countDown();
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
                        String cause = response.has("cause")
                                ? response.get("cause").getAsString()
                                : "Unknown error";
                        sendChat("§8[§cS§8]§7 Failed to fetch data for §c" + name + "§7: " + cause);
                        latch.countDown();
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

                    double score = level * fkdr;

                    PlayerData data = new PlayerData(coloredLevel, coloredPlayerName, formattedFinals, coloredFKDR, score);
                    playerDatas.add(data);

                } catch (Exception e) {
                    sendChat("§8[§cS§8]§7 Error getting stats for " + name);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        new Thread(() -> {
            try {
                latch.await();
                playerDatas.sort(Comparator.comparingDouble((PlayerData p) -> p.score).reversed());

                sendChat("§8[§cS§8]§7 §c§lBed§f§lWars §7stats for current game |");

                for (PlayerData data : playerDatas) {
                    String msg = String.format("%s %s §7| Finals: %s §7| FKDR: %s",
                            data.coloredLevel, data.coloredPlayerName, data.formattedFinals, data.coloredFKDR);
                    sendChat(msg);
                }
            } catch (InterruptedException e) {
                sendChat("§8[§cS§8]§7 Error processing stats: interrupted.");
            }
        }).start();
    }

    private static class PlayerData {
        String coloredLevel;
        String coloredPlayerName;
        String formattedFinals;
        String coloredFKDR;
        double score;

        PlayerData(String coloredLevel, String coloredPlayerName, String formattedFinals, String coloredFKDR, double score) {
            this.coloredLevel = coloredLevel;
            this.coloredPlayerName = coloredPlayerName;
            this.formattedFinals = formattedFinals;
            this.coloredFKDR = coloredFKDR;
            this.score = score;
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