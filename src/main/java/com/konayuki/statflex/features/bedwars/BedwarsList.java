package com.konayuki.statflex.features.bedwars;

import com.konayuki.statflex.utils.*;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.chat.Warn;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.api.HypixelApiUtil;
import com.konayuki.statflex.utils.hypixel.Ranks;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.features.denick.Denick;
import com.konayuki.statflex.features.tab.TabStatsCache;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import com.konayuki.statflex.events.ChatEvent;
import com.konayuki.statflex.events.Subscribe;

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

    @Subscribe
    public void onChat(ChatEvent event) {
        if (!Toggle.isBwList())
            return;

        String raw = event.getMessage().getUnformattedText();
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        String lower = stripped.toLowerCase();
        Minecraft mc = Minecraft.getMinecraft();

        if (lower.startsWith("online:")) {
            if (!Toggle.isKeepWho()) {
                event.setCancelled(true);
            }

            Queue.clear();
            Party.clear();

            names(stripped, Queue);

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
                list(new ArrayList<>(Queue));
                return;
            }

            if (lower.startsWith("party leader") ||
                    lower.startsWith("party moderators")) {
                inParty = true;
                names(stripped, Party);
                return;
            }

            if (lower.startsWith("party members (")) {
                inParty = true;
                names(stripped, Party);
                waitingParty = false;
                list(new ArrayList<>(Queue));
            }
        }
    }

    public static void list(List<String> playerNames) {
        list(playerNames, false);
    }

    public static void list(List<String> rawPlayerNames, boolean forceWarn) {
        if (rawPlayerNames.isEmpty()) return;

        List<String> playerNames = new ArrayList<>();
        for (String name : rawPlayerNames) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (name.equalsIgnoreCase("online")) {
                continue;
            }
            if (Denick.isNick(name)) {
                continue;
            }
            if (!playerNames.contains(name)) {
                playerNames.add(name);
            }
        }
        if (playerNames.isEmpty()) return;

        if (HypixelApiUtil.get().equals("N/A")) {
            Chat.send(Messages.INVALID_API);
            return;
        }

        List<PlayerData> playerDatas = Collections.synchronizedList(new ArrayList<>());
        List<String> failedNames = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(playerNames.size());

        for (String name : playerNames) {
            new Thread(() -> {
                try {
                    HypixelApi.Result result = HypixelApi.fetch(name);
                    if (!result.success) {
                        if (HypixelApi.NAME_NOT_FOUND.equals(result.errorCode)) {
                            Denick.mark(name);
                        } else {
                            failedNames.add(name);
                        }
                        return;
                    }

                    JsonObject player = result.player;
                    String properName = result.properName;

                    if (!player.has("stats") || !player.get("stats").isJsonObject()
                            || !player.getAsJsonObject("stats").has("Bedwars")) {
                        Debug.log(Messages.NO_STATS + name);
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
                            Bedwars.level(level),
                            Ranks.rank(player, properName),
                            Bedwars.finals(finals),
                            Bedwars.fkdr(fkdr),
                            level * fkdr,
                            level,
                            fkdr,
                            finals,
                            properName
                    );

                    playerDatas.add(data);

                } catch (Exception e) {
                    failedNames.add(name);
                } finally {
                    latch.countDown();
                }
            }, "BedwarsList").start();
        }

        new Thread(() -> {
            try {
                latch.await();
                playerDatas.sort(Comparator.comparingDouble(p -> -p.score));

                List<String> lines = new ArrayList<>();
                for (PlayerData data : playerDatas) {
                    lines.add(data.coloredLevel + " " + data.coloredPlayerName + " " + Color.GRAY + "| Finals: "
                            + data.formattedFinals + " " + Color.GRAY + "| FKDR: " + data.coloredFKDR);
                }
                String failedLine = failedNames.isEmpty() ? null : Messages.PREFIX + Color.RED + failedNames.size()
                        + Color.GRAY + "/" + Color.RED + playerNames.size() + Color.GRAY + " failed: " + Color.RED
                        + String.join(Color.GRAY + ", " + Color.RED, failedNames);

                if (TabStatsCache.isTabMode(TabStatsCache.Game.BEDWARS)) {
                    TabStatsCache.set(TabStatsCache.Game.BEDWARS,
                            TabStatsCache.header(TabStatsCache.Game.BEDWARS), lines, failedLine);
                } else {
                    Chat.send(Messages.BEDWARS_STATS);
                    for (String line : lines) {
                        Chat.send(line);
                    }
                    if (failedLine != null) {
                        Chat.send(failedLine);
                    }
                }

                warn(playerDatas, forceWarn);

            } catch (InterruptedException ignored) {
            }
        }, "BedwarsList").start();
    }

    public static void warn(List<PlayerData> playerDatas, boolean forceWarn) {
        int warnLevel = Setting.get().warnLevel;
        double warnFKDR = Setting.get().warnFKDR;

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

            Warn.warn(Warn.format(data));
        }
    }

    private void names(String text, List<String> targetList) {
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
            if (name.equals("online")) {
                continue;
            }
            if (Denick.isNick(name)) {
                continue;
            }
            if (!targetList.contains(name)) {
                targetList.add(name);
            }
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

