package com.konayuki.statflex.features.duels;

import com.konayuki.statflex.utils.Toggle;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.utils.Text;
import com.konayuki.statflex.features.tab.TabStatsCache;

import com.konayuki.statflex.events.ChatEvent;
import com.konayuki.statflex.events.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class DuelsList {
    private static int tickDelay = 0;
    private static String pendingChatLine = null;

    @Subscribe
    public void onChat(ChatEvent event) {
        if (!Toggle.isAuto()) {
            return;
        }

        try {
            String chatLine = event.getMessage().getUnformattedText();
            if (chatLine.contains("Opponent:") || chatLine.contains("Opponents:")) {
                tickDelay = 6;
                pendingChatLine = chatLine;
            }
        } catch (Exception e) {
        }
    }

    @Subscribe
    public void onTick(com.konayuki.statflex.events.TickEvent event) {
        if (!Toggle.isAuto() || pendingChatLine == null) {
            return;
        }

        if (--tickDelay <= 0) {
            process(pendingChatLine);
            pendingChatLine = null;
        }
    }

    private void process(String chatLine) {
        List<String> opponents = parse(chatLine);
        if (opponents.isEmpty()) {
            return;
        }

        Locraw.get().request(new Locraw.LocrawCallback() {
            @Override
            public void onReceived(String gameType, String mode) {
                if ("DUELS".equals(gameType)) {
                    String detectedMode = Duels.detect(mode);
                    if (TabStatsCache.isTabMode(TabStatsCache.Game.DUELS)) {
                        TabStatsCache.begin(TabStatsCache.Game.DUELS);
                    }
                    for (String playerName : opponents) {
                        try {
                            Duels.stats(playerName, detectedMode, true);
                        } catch (Exception e) {
                        }
                    }
                } else {
                }
            }

            @Override
            public void onTimeout() {
            }
        });
    }

    private List<String> parse(String chatLine) {
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

