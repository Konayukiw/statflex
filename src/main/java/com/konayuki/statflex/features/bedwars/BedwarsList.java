package com.konayuki.statflex.features.bedwars;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApiUtil;
import com.konayuki.statflex.utils.api.Profile;
import com.konayuki.statflex.utils.Toggles;
import com.konayuki.statflex.utils.Settings;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Ranks;

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

public class BedwarsList {
    private static final List<String> Queue = new ArrayList<>();
    private static final List<String> Party = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean inParty = false;
    private static volatile boolean waitingParty = false;
    private static final Pattern namePattern = Pattern.compile("\\b[a-zA-Z0-9_]{3,16}\\b");

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Toggles.isListStats())
            return;

        String raw = event.message.getUnformattedText();
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        String lower = stripped.toLowerCase();
        Minecraft mc = Minecraft.getMinecraft();

        if (lower.startsWith("online:")) {
            if (!Toggles.isKeepWho()) {
                event.setCanceled(true);
            }

            Queue.clear();
            Party.clear();

            extractPlayerNames(stripped, Queue);

            waitingParty = true;
            mc.addScheduledTask(() -> {
                if (mc.thePlayer != null) {
                    mc.thePlayer.sendChatMessage("/pl");
                }
            });
            return;
        }

        if (waitingParty) {

            if (lower.contains("not currently in a party")) {
                waitingParty = false;
                inParty = false;
                listBedwarsStats(new ArrayList<>(Queue));
                return;
            }

            if (lower.startsWith("party leader") ||
                    lower.startsWith("party moderators")) {
                inParty = true;
                extractPlayerNames(stripped, Party);
                return;
            }

            if (lower.startsWith("party members (")) {
                inParty = true;
                extractPlayerNames(stripped, Party);
                waitingParty = false;
                listBedwarsStats(new ArrayList<>(Queue));
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

        Matcher matcher = namePattern.matcher(stripped);
        while (matcher.find()) {
            String name = matcher.group();
            if (!targetList.contains(name)) {
                targetList.add(name);
            }
        }
    }

    public static void listBedwarsStats(List<String> playerNames) {
        listBedwarsStats(playerNames, false);
    }

    public static void listBedwarsStats(List<String> playerNames, boolean forceWarn) {
        if (playerNames.isEmpty()) return;

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
                    JsonObject stats = player.getAsJsonObject("stats").getAsJsonObject("Bedwars");

                    int level = player.getAsJsonObject("achievements").get("bedwars_level").getAsInt();
                    int finals = stats.has("final_kills_bedwars") ? stats.get("final_kills_bedwars").getAsInt() : 0;
                    int deaths = stats.has("final_deaths_bedwars") ? stats.get("final_deaths_bedwars").getAsInt() : 1;

                    double fkdr = deaths == 0 ? finals : (double) finals / deaths;

                    PlayerData data = new PlayerData(
                            Bedwars.getColoredLevel(level),
                            Ranks.getColoredPlayerName(player, properName),
                            Bedwars.getFormattedFinals(finals),
                            Bedwars.getColoredFKDR(fkdr),
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

                Chat.send(Messages.BEDWARS_STATS);

                for (PlayerData data : playerDatas) {
                    Chat.send(data.coloredLevel + " " + data.coloredPlayerName + " §7| Finals: " + data.formattedFinals + " §7| FKDR: " + data.coloredFKDR);
                }

                maybeWarnPlayers(playerDatas, forceWarn);

            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    public static void maybeWarnPlayers(List<PlayerData> playerDatas, boolean forceWarn) {
        int warnLevel = Settings.getInstance().warnLevel;
        double warnFKDR = Settings.getInstance().warnFKDR;

        boolean useLevel = warnLevel > 0;
        boolean useFKDR = warnFKDR > 0;
        if (!useLevel && !useFKDR) {
            return;
        }
        if (!forceWarn && !inParty) {
            return;
        }

        Set<String> partySet = forceWarn ? Collections.emptySet() : new HashSet<>(Party);

        for (PlayerData data : playerDatas) {
            if (!forceWarn && partySet.contains(data.plainName.toLowerCase())) {
                continue;
            }

            boolean matches = true;
            if (useLevel) {
                matches &= data.level >= warnLevel;
            }
            if (useFKDR) {
                matches &= data.fkdr >= warnFKDR;
            }
            if (!matches) {
                continue;
            }

            sendPartyWarn(buildStatsWarnMessage(data));
        }
    }

    public static void warnNickedPlayer(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return;
        }
        int warnLevel = Settings.getInstance().warnLevel;
        double warnFKDR = Settings.getInstance().warnFKDR;
        if (warnLevel <= 0 && warnFKDR <= 0) {
            return;
        }
        sendPartyWarn("NICKED: " + displayName);
    }

    public static void fetchAndWarn(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        int warnLevel = Settings.getInstance().warnLevel;
        double warnFKDR = Settings.getInstance().warnFKDR;
        if (warnLevel <= 0 && warnFKDR <= 0) {
            return;
        }

        new Thread(() -> {
            try {
                String apiKey = HypixelApiUtil.getApiKey();
                Profile.PlayerInfo info = Profile.getPlayerInfo(playerName);
                if (info == null) {
                    return;
                }

                HttpURLConnection connection = (HttpURLConnection)
                        new URL("https://api.hypixel.net/player?key=" + apiKey + "&uuid=" + info.uuid).openConnection();
                connection.setRequestMethod("GET");

                int status = connection.getResponseCode();
                InputStreamReader reader = status >= 200 && status < 300
                        ? new InputStreamReader(connection.getInputStream())
                        : new InputStreamReader(connection.getErrorStream());

                JsonObject response = new JsonParser().parse(reader).getAsJsonObject();
                if (!response.get("success").getAsBoolean()) {
                    return;
                }

                JsonObject player = response.getAsJsonObject("player");
                if (player == null || !player.has("stats") || !player.getAsJsonObject("stats").has("Bedwars")) {
                    return;
                }
                JsonObject stats = player.getAsJsonObject("stats").getAsJsonObject("Bedwars");

                int level = player.has("achievements") && player.getAsJsonObject("achievements").has("bedwars_level")
                        ? player.getAsJsonObject("achievements").get("bedwars_level").getAsInt()
                        : 0;
                int finals = stats.has("final_kills_bedwars") ? stats.get("final_kills_bedwars").getAsInt() : 0;
                int deaths = stats.has("final_deaths_bedwars") ? stats.get("final_deaths_bedwars").getAsInt() : 1;
                double fkdr = deaths == 0 ? finals : (double) finals / deaths;

                boolean matches = true;
                if (warnLevel > 0) {
                    matches &= level >= warnLevel;
                }
                if (warnFKDR > 0) {
                    matches &= fkdr >= warnFKDR;
                }
                if (!matches) {
                    return;
                }

                PlayerData data = new PlayerData(
                        Bedwars.getColoredLevel(level),
                        Ranks.getColoredPlayerName(player, info.name),
                        Bedwars.getFormattedFinals(finals),
                        Bedwars.getColoredFKDR(fkdr),
                        level * fkdr,
                        level,
                        fkdr,
                        finals,
                        info.name
                );
                sendPartyWarn(buildStatsWarnMessage(data));
            } catch (Exception ignored) {
            }
        }).start();
    }

    public static String buildStatsWarnMessage(PlayerData data) {
        String starIcon = data.level <= 1099 ? "✫" : "✪";
        String plainLevel = Bedwars.formatLevelPlain(data.level);
        String plainFinals = Bedwars.formatFinalsPlain(data.finals);
        String plainFKDR = Bedwars.formatFKDRPlain(data.fkdr);
        return starIcon + plainLevel + " " + data.plainName + " | Finals: " + plainFinals + " | FKDR: " + plainFKDR;
    }

    private static void sendPartyWarn(String warning) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }

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

    public static class PlayerData {
        public String coloredLevel;
        public String coloredPlayerName;
        public String formattedFinals;
        public String coloredFKDR;
        public double score;
        public int level;
        public double fkdr;
        public int finals;
        public String plainName;

        public PlayerData(String cl, String cp, String ff, String cf,
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

