package com.konayuki.statflex.features.duels;

import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Locraw;
import com.konayuki.statflex.utils.Toggles;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class DuelsList {

    private static int tickDelay = 0;
    private static String pendingChatLine = null;

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!Toggles.isAutoStatsEnabled()) {
            return;
        }

        try {
            String chatLine = event.message.getUnformattedText();
            if (chatLine.contains("Opponent:") || chatLine.contains("Opponents:")) {
                tickDelay = 6;
                pendingChatLine = chatLine;
            }
        } catch (Exception e) {
            Debug.log("Failed to handle chat: " + e.getClass().getSimpleName());
        }
    }

    @SubscribeEvent
    public void onClientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (!Toggles.isAutoStatsEnabled() || pendingChatLine == null) {
            return;
        }

        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
            return;
        }

        if (--tickDelay <= 0) {
            processOpponents(pendingChatLine);
            pendingChatLine = null;
        }
    }

    private void processOpponents(String chatLine) {
        List<String> opponents = parseOpponents(chatLine);
        if (opponents.isEmpty()) {
            return;
        }

        // Request locraw to get current game mode
        Locraw.getInstance().requestLocraw(new Locraw.LocrawCallback() {
            @Override
            public void onLocrawReceived(String gameType, String mode) {
                if ("DUELS".equals(gameType)) {
                    String detectedMode = Duels.detectModeFromLocraw(mode);
                    for (String playerName : opponents) {
                        try {
                            Duels.fetchStats(playerName, detectedMode, true);
                        } catch (Exception e) {
                            Debug.log("Failed to fetch stats for " + playerName + ": " + e.getMessage());
                        }
                    }
                } else {
                    Debug.log("Not in Duels game type: " + gameType);
                }
            }

            @Override
            public void onLocrawTimeout() {
                Debug.log("Locraw timeout when trying to get game mode for auto stats");
            }
        });
    }

    private List<String> parseOpponents(String chatLine) {
        List<String> opponents = new ArrayList<>();
        try {
            if (!chatLine.contains("Opponent:") && !chatLine.contains("Opponents:")) {
                return opponents;
            }

            String line = stripColorCodes(chatLine).replaceFirst("(?i)Opponents?:", "").trim();
            line = line.replaceAll("\\[[^\\]]*\\]", "").trim();

            String[] names = line.split(",");
            for (String name : names) {
                name = name.trim().split(" ")[0];
                if (!name.isEmpty()) {
                    opponents.add(name);
                }
            }
        } catch (Exception e) {
            Debug.log("Failed to parse opponents: " + e.getMessage());
        }
        return opponents;
    }

    private static String stripColorCodes(String input) {
        if (input == null)
            return "";
        return input.replaceAll("§.", "");
    }
}

