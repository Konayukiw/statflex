package com.konayuki.statflex.features.skywars;

import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Toggle;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.api.HypixelApi;
import com.konayuki.statflex.utils.hypixel.Ranks;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.features.denick.Denick;

import com.google.gson.JsonObject;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkywarsList {
    private static final int COLLECT_TIMEOUT_TICKS = 5;
    private static final Pattern TEAM_LINE = Pattern.compile("(?i)^Team\\s*#(\\d+)\\s*:\\s*(.*)$");
    private static final Pattern namePattern = Pattern.compile("\\b[a-zA-Z0-9_]{3,16}\\b");

    private final List<String> queue = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, PlayerData> fetched = new ConcurrentHashMap<>();
    private final Set<String> fetchStarted = ConcurrentHashMap.newKeySet();
    private final AtomicInteger pendingFetches = new AtomicInteger(0);
    private final AtomicBoolean collecting = new AtomicBoolean(false);
    private final AtomicBoolean collectionDone = new AtomicBoolean(false);
    private final AtomicBoolean listDisplayed = new AtomicBoolean(false);
    private final Set<String> failedNames = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean apiErrorReported = new AtomicBoolean(false);

    private volatile int lastTeamNumber = 0;
    private volatile int ticksSinceLastTeam = 0;

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Toggle.isSkywarsListStats()) {
            return;
        }

        String raw = event.message.getUnformattedText();
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(raw);
        if (stripped == null) {
            return;
        }
        String trimmed = stripped.trim();
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("online:")) {
            if (!Toggle.isKeepWho()) {
                event.setCanceled(true);
            }
            resetSession();
            List<String> names = new ArrayList<>();
            extractPlayerNames(stripped, names);
            for (String name : names) {
                enqueuePlayer(name);
            }
            collectionDone.set(true);
            collecting.set(false);
            tryDisplayList();
            return;
        }

        if (lower.startsWith("mode:")) {
            if (!Toggle.isKeepWho()) {
                event.setCanceled(true);
            }
            beginCollection();
            return;
        }

        if (!collecting.get()) {
            return;
        }

        Matcher teamMatcher = TEAM_LINE.matcher(trimmed);
        if (teamMatcher.matches()) {
            int teamNum;
            try {
                teamNum = Integer.parseInt(teamMatcher.group(1));
            } catch (NumberFormatException e) {
                return;
            }

            if (!Toggle.isKeepWho()) {
                event.setCanceled(true);
            }

            lastTeamNumber = Math.max(lastTeamNumber, teamNum);
            ticksSinceLastTeam = 0;

            String playersPart = teamMatcher.group(2);
            List<String> names = new ArrayList<>();
            extractPlayerNames(playersPart, names);
            for (String name : names) {
                enqueuePlayer(name);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!Toggle.isSkywarsListStats() || !collecting.get() || collectionDone.get()) {
            return;
        }

        if (lastTeamNumber <= 0) {
            return;
        }
        ticksSinceLastTeam++;
        if (ticksSinceLastTeam >= COLLECT_TIMEOUT_TICKS) {
            collectionDone.set(true);
            collecting.set(false);
            tryDisplayList();
        }
    }

    private void beginCollection() {
        resetSession();
        collecting.set(true);
        collectionDone.set(false);
        lastTeamNumber = 0;
        ticksSinceLastTeam = 0;
    }

    private void resetSession() {
        queue.clear();
        fetched.clear();
        fetchStarted.clear();
        failedNames.clear();
        apiErrorReported.set(false);
        pendingFetches.set(0);
        collecting.set(false);
        collectionDone.set(false);
        listDisplayed.set(false);
        lastTeamNumber = 0;
        ticksSinceLastTeam = 0;
    }

    private void enqueuePlayer(String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        if (Denick.isNicked(name)) {
            return;
        }
        String key = name.toLowerCase();
        synchronized (queue) {
            if (!queue.contains(key)) {
                queue.add(key);
            }
        }
        startFetchIfNeeded(key);
    }

    private void startFetchIfNeeded(String nameKey) {
        if (!fetchStarted.add(nameKey)) {
            return;
        }
        pendingFetches.incrementAndGet();
        new Thread(() -> {
            try {
                HypixelApi.result result = HypixelApi.fetch(nameKey);
                if (!result.success) {
                    if (HypixelApi.INVALID_API.equals(result.errorCode)) {
                        if (apiErrorReported.compareAndSet(false, true)) {
                            Chat.send(Messages.INVALID_API);
                        }
                    } else {
                        Debug.log("Failed to fetch " + nameKey + ": " + result.errorCode);
                        failedNames.add(nameKey);
                    }
                    return;
                }

                PlayerData data = buildPlayerData(result.player, result.properName);
                if (data != null) {
                    fetched.put(nameKey, data);
                } else {
                    failedNames.add(nameKey);
                }
            } finally {
                pendingFetches.decrementAndGet();
                tryDisplayList();
            }
        }, "SkywarsList").start();
    }

    private void tryDisplayList() {
        if (!collectionDone.get()) {
            return;
        }
        if (pendingFetches.get() > 0) {
            return;
        }
        if (!listDisplayed.compareAndSet(false, true)) {
            return;
        }

        List<PlayerData> playerDatas = new ArrayList<>();
        synchronized (queue) {
            for (String name : queue) {
                PlayerData data = fetched.get(name);
                if (data != null) {
                    playerDatas.add(data);
                }
            }
        }

        if (playerDatas.isEmpty()) {
            return;
        }

        playerDatas.sort(Comparator.comparingDouble(p -> -p.score));

        Chat.send(Messages.SKYWARS_STATS);
        for (PlayerData data : playerDatas) {
            Chat.send(data.levelFormatted + " " + data.coloredPlayerName
                    + Color.GRAY + " | Wins: " + data.formattedWins + Color.GRAY + " | KDR: " + data.coloredKDR);
        }

        if (!failedNames.isEmpty()) {
            int total;
            synchronized (queue) {
                total = queue.size();
            }
            Chat.send(Messages.PREFIX + Color.RED + failedNames.size() + Color.GRAY + "/" + Color.RED + total
                    + Color.GRAY + " failed: " + Color.RED
                    + String.join(Color.GRAY + ", " + Color.RED, failedNames));
        }
    }

    private static PlayerData buildPlayerData(JsonObject player, String properName) {
        if (!player.has("stats") || !player.get("stats").isJsonObject()) {
            return null;
        }

        JsonObject statsRoot = player.getAsJsonObject("stats");
        if (!statsRoot.has("SkyWars") || !statsRoot.get("SkyWars").isJsonObject()) {
            return null;
        }

        JsonObject stats = statsRoot.getAsJsonObject("SkyWars");

        String rawFormatted = stats.has("levelFormattedWithBrackets")
                ? stats.get("levelFormattedWithBrackets").getAsString()
                : Color.GRAY + "[N/A]";

        int wins = stats.has("wins") ? stats.get("wins").getAsInt() : 0;
        int kills = stats.has("kills") ? stats.get("kills").getAsInt() : 0;
        int deaths = stats.has("deaths") ? stats.get("deaths").getAsInt() : 1;
        double kdr = deaths == 0 ? kills : (double) kills / deaths;

        String coloredPlayerName = Ranks.rank(player, properName);
        String formattedWins = Skywars.getFormattedWins(wins);
        String coloredKDR = Skywars.getColoredKDR(kdr);
        double score = parseLevelNumber(rawFormatted) * kdr;

        return new PlayerData(
                rawFormatted,
                coloredPlayerName,
                formattedWins,
                coloredKDR,
                score,
                properName
        );
    }

    private void extractPlayerNames(String text, List<String> targetList) {
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        if (stripped == null) {
            stripped = text;
        }
        stripped = stripped.toLowerCase();

        stripped = stripped
                .replaceAll("(?i)team\\s*#\\d+\\s*:", "")
                .replaceAll("(?i)online:", "")
                .replaceAll("(?i)mode:", "")
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
