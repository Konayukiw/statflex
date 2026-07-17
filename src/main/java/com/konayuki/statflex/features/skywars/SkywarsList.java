package com.konayuki.statflex.features.skywars;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApiUtil;
import com.konayuki.statflex.utils.api.Profile;
import com.konayuki.statflex.utils.Toggles;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Ranks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

public class SkywarsList {
    private static final List<String> Queue = new ArrayList<>();
    private static final Pattern namePattern = Pattern.compile("\\b[a-zA-Z0-9_]{3,16}\\b");

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Toggles.isSkywarsListStats()) {
            return;
        }

        String raw = event.message.getUnformattedText();
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        String lower = stripped.toLowerCase();

        if (lower.startsWith("online:")) {
            if (!Toggles.isKeepWho() && !Toggles.isListStats()) {
                event.setCanceled(true);
            }

            Queue.clear();
            extractPlayerNames(stripped, Queue);
            listSkywarsStats(new ArrayList<>(Queue));
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

        Matcher matcher = namePattern.matcher(stripped);
        while (matcher.find()) {
            String name = matcher.group();
            if (!targetList.contains(name)) {
                targetList.add(name);
            }
        }
    }

    public static void listSkywarsStats(List<String> playerNames) {
        if (playerNames == null || playerNames.isEmpty()) {
            return;
        }

        List<PlayerData> playerDatas = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(playerNames.size());

        for (String name : playerNames) {
            new Thread(() -> {
                try {
                    String apiKey = HypixelApiUtil.getApiKey();
                    Profile.PlayerInfo info = Profile.getPlayerInfo(name);

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
                    if (player == null || !player.has("stats") || !player.get("stats").isJsonObject()) {
                        latch.countDown();
                        return;
                    }

                    JsonObject statsRoot = player.getAsJsonObject("stats");
                    if (!statsRoot.has("SkyWars") || !statsRoot.get("SkyWars").isJsonObject()) {
                        latch.countDown();
                        return;
                    }

                    JsonObject stats = statsRoot.getAsJsonObject("SkyWars");

                    String rawFormatted = stats.has("levelFormattedWithBrackets")
                            ? stats.get("levelFormattedWithBrackets").getAsString()
                            : "§7[N/A]";
                    String levelFormatted = Skywars.sanitizeFormattedLevel(rawFormatted);

                    int wins = stats.has("wins") ? stats.get("wins").getAsInt() : 0;
                    int kills = stats.has("kills") ? stats.get("kills").getAsInt() : 0;
                    int deaths = stats.has("deaths") ? stats.get("deaths").getAsInt() : 1;
                    double kdr = deaths == 0 ? kills : (double) kills / deaths;

                    String coloredPlayerName = Ranks.getColoredPlayerName(player, properName);
                    String formattedWins = Skywars.getFormattedWins(wins);
                    String coloredKDR = Skywars.getColoredKDR(kdr);

                    double score = parseLevelNumber(rawFormatted) * kdr;

                    PlayerData data = new PlayerData(
                            levelFormatted,
                            coloredPlayerName,
                            formattedWins,
                            coloredKDR,
                            score,
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

                Chat.send(Messages.SKYWARS_STATS);

                for (PlayerData data : playerDatas) {
                    Chat.send(data.levelFormatted + " " + data.coloredPlayerName
                            + " §7| Wins: " + data.formattedWins
                            + " §7| KDR: " + data.coloredKDR);
                }
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private static double parseLevelNumber(String rawFormatted) {
        if (rawFormatted == null || rawFormatted.isEmpty()) {
            return 0;
        }
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(rawFormatted);
        if (plain == null) {
            plain = rawFormatted.replaceAll("§.", "");
        }
        StringBuilder digits = new StringBuilder();
        boolean sawDigit = false;
        boolean sawDot = false;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
                sawDigit = true;
            } else if (c == '.' && sawDigit && !sawDot) {
                digits.append(c);
                sawDot = true;
            } else if (sawDigit) {
                break;
            }
        }
        if (digits.length() == 0) {
            return 0;
        }
        try {
            return Double.parseDouble(digits.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static class PlayerData {
        final String levelFormatted;
        final String coloredPlayerName;
        final String formattedWins;
        final String coloredKDR;
        final double score;
        final String plainName;

        PlayerData(String levelFormatted, String coloredPlayerName, String formattedWins,
                   String coloredKDR, double score, String plainName) {
            this.levelFormatted = levelFormatted;
            this.coloredPlayerName = coloredPlayerName;
            this.formattedWins = formattedWins;
            this.coloredKDR = coloredKDR;
            this.score = score;
            this.plainName = plainName;
        }
    }
}
