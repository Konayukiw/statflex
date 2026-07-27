package com.konayuki.statflex.features.duels;

import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.utils.Messages;
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
        if (!Toggles.isAutoStats()) {
            return;
        }

        try {
            String chatLine = event.message.getUnformattedText();
            if (chatLine.contains("Opponent:") || chatLine.contains("Opponents:")) {
                tickDelay = 6;
                pendingChatLine = chatLine;
            }
        } catch (Exception e) {
            Debug.log("[S] Failed to read chat: " + e.getClass().getSimpleName());
        }
    }

    @SubscribeEvent
    public void onClientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (!Toggles.isAutoStats() || pendingChatLine == null) {
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

        Locraw.getInstance().requestLocraw(new Locraw.LocrawCallback() {
            @Override
            public void onLocrawReceived(String gameType, String mode) {
                if ("DUELS".equals(gameType)) {
                    String detectedMode = Duels.detectModeFromLocraw(mode);
                    for (String playerName : opponents) {
                        try {
                            Duels.fetchStats(playerName, detectedMode, true);
                        } catch (Exception e) {
                            Debug.log(Messages.FETCH_ERROR + playerName + ": " + e.getMessage());
                        }
                    }
                } else {
                    Debug.log("[S] You are not in Duels: " + gameType);
                }
            }

            @Override
            public void onLocrawTimeout() {
                Debug.log("[S] Locraw timeout while trying to get game mode");
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
            Debug.log("[S] Failed to parse opponents: " + e.getMessage());
        }
        return opponents;
    }

    private static String stripColorCodes(String input) {
        if (input == null)
            return "";
        return input.replaceAll("§.", "");
    }
}

