package com.konayuki.statflex.stats.bedwars;

import com.konayuki.statflex.client.ChatManager;
import com.konayuki.statflex.config.ApiKeyManager;
import com.konayuki.statflex.config.Toggles;
import com.konayuki.statflex.config.Settings;
import com.konayuki.statflex.system.ProfileHandler;
import com.konayuki.statflex.system.Messages;
import com.konayuki.statflex.system.Formatter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedwarsStatsLister {
    private static final List<String> collectedPlayers = new ArrayList<>();

    private static final List<String> partyMembers = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean inParty = false;
    private static volatile boolean waitingForParty = false;
    private static final Pattern playerNamePattern = Pattern.compile("\\b[a-zA-Z0-9_]{3,16}\\b");

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Toggles.isListStatsEnabled())
            return;

        String raw = event.message.getUnformattedText();
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        String lower = stripped.toLowerCase();

        if (lower.startsWith("online:")) {
            if (!Toggles.isKeepWhoEnabled()) {
                event.setCanceled(true);
            }

            collectedPlayers.clear();
            partyMembers.clear();

            extractPlayerNames(stripped, collectedPlayers);

            waitingForParty = true;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().thePlayer != null) {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/pl");
                }
            });
            return;
        }

        if (waitingForParty) {

            if (lower.contains("not currently in a party")) {
                waitingForParty = false;
                inParty = false;
                listBedwarsStats(new ArrayList<>(collectedPlayers));
                return;
            }

            if (lower.startsWith("party leader") ||
                    lower.startsWith("party moderators")) {
                inParty = true;
                extractPlayerNames(stripped, partyMembers);
                return;
            }

            if (lower.startsWith("party members (")) {
                inParty = true;
                extractPlayerNames(stripped, partyMembers);
                waitingForParty = false;
                listBedwarsStats(new ArrayList<>(collectedPlayers));
            }
        }
    }

    private void extractPlayerNames(String text, List<String> targetList) {
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text).toLowerCase();

        stripped = stripped
                .replace("party leader", "")
                .replace("party members", "")
                .replace("party members:", "")
                .replaceAll("\\[vip\\+\\+\\]|\\[vip\\+\\]|\\[vip\\]|\\[mvp\\+\\+\\]|\\[mvp\\+\\]|\\[mvp\\]|\\[youtube\\]", "");

        stripped = stripped.replaceAll("[^a-z0-9_ ]", " ");

        Matcher matcher = playerNamePattern.matcher(stripped);
        while (matcher.find()) {
            String name = matcher.group();
            if (!targetList.contains(name)) {
                targetList.add(name);
            }
        }
    }

    public static void listBedwarsStats(List<String> playerNames) {
        if (playerNames.isEmpty()) return;

        List<PlayerData> playerDatas = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(playerNames.size());

        for (String name : playerNames) {
            new Thread(() -> {
                try {
                    String apiKey = ApiKeyManager.getApiKey();
                    ProfileHandler.PlayerInfo info = ProfileHandler.getPlayerInfo(name);

                    if (info == null) {
                        latch.countDown();
                        return;
                    }

                    String uuid = info.uuid;
                    String properName = info.name;

                    HttpURLConnection connection = (HttpURLConnection)
                            new URL("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + uuid).openConnection();
                    connection.setRequestMethod("GET");

                    int status = connection.getResponseCode();
                    InputStreamReader reader = status >= 200 && status < 300
                            ? new InputStreamReader(connection.getInputStream())
                            : new InputStreamReader(connection.getErrorStream());

                    JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
                    if (!response.get("success").getAsBoolean()) {
                        latch.countDown();
                        return;
                    }

                    JsonObject player = response.getAsJsonObject("player");
                    JsonObject stats = player.getAsJsonObject("stats").getAsJsonObject("Bedwars");

                    int level = player.getAsJsonObject("achievements").get("bedwars_level").getAsInt();
                    int finals = stats.has("final_kills_bedwars") ? stats.get("final_kills_bedwars").getAsInt() : 0;
                    int deaths = stats.has("final_deaths_bedwars") ? stats.get("final_deaths_bedwars").getAsInt() : 1;

                    double fkdr = deaths == 0 ? finals : (double) finals / deaths;

                    PlayerData data = new PlayerData(
                            BedwarsFetcher.getColoredLevel(level),
                            Formatter.getColoredPlayerName(player, properName),
                            BedwarsFetcher.getFormattedFinals(finals),
                            BedwarsFetcher.getColoredFKDR(fkdr),
                            level * fkdr,
                            level,
                            fkdr,
                            finals,
                            properName
                    );

                    playerDatas.add(data);

                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        new Thread(() -> {
            try {
                latch.await();
                playerDatas.sort(Comparator.comparingDouble(p -> -p.score));

                ChatManager.send(Messages.BEDWARS_STATS);

                for (PlayerData data : playerDatas) {
                    ChatManager.send(data.coloredLevel + " " + data.coloredPlayerName + " §7| Finals: " + data.formattedFinals + " §7| FKDR: " + data.coloredFKDR);
                }

                int warnLevel = Settings.getInstance().warnLevel;
                double warnFKDR = Settings.getInstance().warnFKDR;

                boolean useLevel = warnLevel > 0;
                boolean useFKDR = warnFKDR > 0;

                if (inParty && (useLevel || useFKDR)) {
                    Set<String> partySet = new HashSet<>(partyMembers);

                    for (PlayerData data : playerDatas) {
                        if (!partySet.contains(data.plainName.toLowerCase())) {
                            boolean matches = true;

                            if (useLevel) {
                                matches &= data.level >= warnLevel;
                            }

                            if (useFKDR) {
                                matches &= data.fkdr >= warnFKDR;
                            }

                            if (matches) {
                                String starIcon = data.level <= 1099 ? "✫" : "✪";
                                String plainLevel = BedwarsFetcher.formatLevelPlain(data.level);
                                String plainFinals = BedwarsFetcher.formatFinalsPlain(data.finals);
                                String plainFKDR = BedwarsFetcher.formatFKDRPlain(data.fkdr);
                                String warning = starIcon + plainLevel + " " + data.plainName + " | Finals: " + plainFinals + " | FKDR: " + plainFKDR;

                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException ignored) {}

                                Minecraft.getMinecraft().addScheduledTask(() -> {
                                    if (Minecraft.getMinecraft().thePlayer != null) {
                                        Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc " + warning);
                                    }
                                });

                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    }
                }

            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private static class PlayerData {
        String coloredLevel;
        String coloredPlayerName;
        String formattedFinals;
        String coloredFKDR;
        double score;
        int level;
        double fkdr;
        int finals;
        String plainName;

        PlayerData(String cl, String cp, String ff, String cf,
                   double score, int level, double fkdr, int finals, String plainName) {
            this.coloredLevel = cl;
            this.coloredPlayerName = cp;
            this.formattedFinals = ff;
            this.coloredFKDR = cf;
            this.score = score;
            this.level = level;
            this.fkdr = fkdr;
            this.finals = finals;
            this.plainName = plainName;
        }
    }
}

