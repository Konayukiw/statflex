package com.dev.statflex;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class DuelsListStats {

    private static String currentMode = null;

    private static boolean waitingForScoreboard = false;
    private static int scoreboardDelayTicks = 0;
    private static String pendingChatLine = null;

    public static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        try {
            String chatLine = event.message.getUnformattedText();
            if ((chatLine.contains("Opponent:") || chatLine.contains("Opponents:"))
                    && Fetcher.isAutoStatsEnabled()) {
                waitingForScoreboard = true;
                scoreboardDelayTicks = 6;
                pendingChatLine = chatLine;
            }
        } catch (Exception e) {
            sendChat("§8[§cS§8]§7 Failed to read chat: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (waitingForScoreboard && event.phase == TickEvent.Phase.END) {
            if (--scoreboardDelayTicks <= 0) {
                waitingForScoreboard = false;
                processWithScoreboard(pendingChatLine);
                pendingChatLine = null;
            }
        }
    }

    private void processWithScoreboard(String chatLine) {
        try {
            List<String> scoreboardLines = DuelsFetcher.getScoreboardLines();
            if (scoreboardLines.isEmpty()) {
                System.out.println("[S] Scoreboard is empty.");
            } else {
                System.out.println("[S] Read scoreboard:");
                for (String line : scoreboardLines) {
                    System.out.println("[S] 'Mode': " + line);
                }
            }
            updateModeFromScoreboardLines(scoreboardLines);
            getFormattedFromChat(chatLine);
        } catch (Exception e) {
            System.out.println("[S] Failed to process scoreboard: " + e);
            e.printStackTrace();
        }
    }

    public static void updateModeFromScoreboardLines(List<String> scoreboardLines) {
        try {
            if (scoreboardLines == null || scoreboardLines.isEmpty()) {
                sendChat("§8[§cS§8]§7 Scoreboard is empty");
                currentMode = null;
                return;
            }

            boolean modeFound = false;

            for (int i = scoreboardLines.size() - 1; i >= 0; i--) {
                String line = scoreboardLines.get(i);
                String cleanedLine = stripColorCodes(line).toLowerCase().trim();

                if (cleanedLine.contains("mode:") || cleanedLine.contains("game:") || cleanedLine.contains("type:")) {
                    String rawMode = cleanedLine.replaceFirst("(?i)(mode:|game:|type:)", "").trim();
                    if (rawMode.startsWith("bed")) {
                        if (rawMode.startsWith("bed wars")) {
                            currentMode = DuelsFetcher.getModeKey("bed");
                        } else if (rawMode.startsWith("bed rush")) {
                            currentMode = DuelsFetcher.getModeKey("rush");
                        }
                    } else {
                        String firstWord = rawMode.split(" ")[0];
                        currentMode = DuelsFetcher.getModeKey(firstWord.toLowerCase());
                    }
                    modeFound = true;
                    break;
                }
            }

            if (!modeFound) {
                sendChat("§8[§cS§8]§7 Failed to detect mode from scoreboard.");
                currentMode = null;
            }
        } catch (Exception e) {
            sendChat("§8[§cS§8]§7 Failed to read scoreboard " + e.getClass().getSimpleName()
                    + ": " + e.getMessage());
            currentMode = null;
            e.printStackTrace();
        }
    }

    public static List<String> parseOpponents(String chatLine) {
        List<String> opponents = new ArrayList<>();
        try {
            if (!chatLine.contains("Opponent:") && !chatLine.contains("Opponents:")) {
                return opponents;
            }

            String line = stripColorCodes(chatLine).replaceFirst("(?i)Opponents?:", "").trim();
            line = line.replaceAll("\\[[^\\]]+\\]", "").trim();

            String[] names = line.split(",");
            for (String name : names) {
                name = name.trim().split(" ")[0];
                if (!name.isEmpty()) {
                    opponents.add(name);
                }
            }
        } catch (Exception e) {
            sendChat("§8[§cS§8]§7 Failed to detect opponents: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return opponents;
    }

    public static void getFormattedFromChat(String chatLine) {
        try {
            List<String> opponents = parseOpponents(chatLine);

            for (String playerName : opponents) {
                try {
                    if (SettingsManager.getInstance().duelsUpdate && Fetcher.isAutoStatsEnabled()) {
                        DuelsFetcherForUpdate.fetchStats(playerName, currentMode, true);
                    } else {
                        DuelsFetcher.fetchStats(playerName, currentMode, true);
                    }
                } catch (Exception e) {
                    sendChat("§8[§cS§8]§7 Failed to fetch stats for " + playerName + ": "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            sendChat("§8[§cS§8]§7 Failed to format info for chat: " + e.getClass().getSimpleName() + ": "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String stripColorCodes(String input) {
        if (input == null)
            return "";
        return input.replaceAll("§.", "");
    }
}
