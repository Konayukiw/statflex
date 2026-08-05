package com.konayuki.statflex.features.duels;

import com.konayuki.statflex.utils.Toggle;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.utils.Text;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class DuelsList {

    private static int tickDelay = 0;
    private static String pendingChatLine = null;

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!Toggle.isAutoStats()) {
            return;
        }

        try {
            String chatLine = event.message.getUnformattedText();
            if (chatLine.contains("Opponent:") || chatLine.contains("Opponents:")) {
                tickDelay = 6;
                pendingChatLine = chatLine;
            }
        } catch (Exception e) {
        }
    }

    @SubscribeEvent
    public void onClientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (!Toggle.isAutoStats() || pendingChatLine == null) {
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

        Locraw.getInstance().sendLocraw(new Locraw.LocrawCallback() {
            @Override
            public void onLocrawReceived(String gameType, String mode) {
                if ("DUELS".equals(gameType)) {
                    String detectedMode = Duels.detectMode(mode);
                    for (String playerName : opponents) {
                        try {
                            Duels.fetchStats(playerName, detectedMode, true);
                        } catch (Exception e) {
                        }
                    }
                } else {
                }
            }

            @Override
            public void onLocrawTimeout() {
            }
        });
    }

    private List<String> parseOpponents(String chatLine) {
        List<String> opponents = new ArrayList<>();
        try {
            if (!chatLine.contains("Opponent:") && !chatLine.contains("Opponents:")) {
                return opponents;
            }

            String line = Text.strip(chatLine).replaceFirst("(?i)Opponents?:", "").trim();
            line = line.replaceAll("\\[[^\\]]*\\]", "").trim();

            String[] names = line.split(",");
            for (String name : names) {
                name = name.trim().split(" ")[0];
                if (!name.isEmpty()) {
                    opponents.add(name);
                }
            }
        } catch (Exception e) {
        }
        return opponents;
    }
}

