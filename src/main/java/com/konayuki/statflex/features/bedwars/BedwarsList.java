package com.konayuki.statflex.features.bedwars;

import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.api.HypixelApiUtil;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Toggles;
import com.konayuki.statflex.utils.Settings;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.chat.Warn;
import com.konayuki.statflex.utils.Ranks;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

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

        if (HypixelApiUtil.getApiKey().equals("N/A")) {
            Chat.send(Messages.INVALID_API);
            return;
        }

        List<PlayerData> playerDatas = Collections.synchronizedList(new ArrayList<>());
        List<String> failedNames = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(playerNames.size());

        for (String name : playerNames) {
            new Thread(() -> {
                try {
                    HypixelApi.result result = HypixelApi.Fetch(name);
                    if (!result.success) {
                        Debug.log("Failed to fetch " + name + ": " + result.errorCode);
                        failedNames.add(name);
                        return;
                    }

                    JsonObject player = result.player;
                    String properName = result.properName;

                    if (!player.has("stats") || !player.get("stats").isJsonObject()
                            || !player.getAsJsonObject("stats").has("Bedwars")) {
                        Debug.log("No Bedwars stats for " + name);
                        failedNames.add(name);
                        return;
                    }
                    JsonObject stats = player.getAsJsonObject("stats").getAsJsonObject("Bedwars");

                    int level = player.has("achievements") && player.get("achievements").isJsonObject()
                            && player.getAsJsonObject("achievements").has("bedwars_level")
                            ? player.getAsJsonObject("achievements").get("bedwars_level").getAsInt()
                            : 0;
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

                } catch (Exception e) {
                    Debug.log("Failed to fetch " + name + ": " + e.getClass().getSimpleName());
                    failedNames.add(name);
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

                if (!failedNames.isEmpty()) {
                    Chat.send(Messages.PREFIX + "§c" + failedNames.size() + "§7/§c" + playerNames.size()
                            + "§7 failed: §c" + String.join("§7, §c", failedNames));
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

            Warn.sendWarning(Warn.buildStatWarning(data));
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

