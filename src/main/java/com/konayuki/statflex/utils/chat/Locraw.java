package com.konayuki.statflex.utils.chat;

import com.konayuki.statflex.utils.Debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import com.konayuki.statflex.events.ChatEvent;
import com.konayuki.statflex.events.Subscribe;
import com.konayuki.statflex.events.TickEvent;

import java.util.ArrayList;
import java.util.List;

public class Locraw {
    private static Locraw instance;

    private boolean awaitingLocraw = false;
    private String gameType = null;
    private String mode = null;
    private int locrawTimeout = 0;
    private final List<LocrawCallback> pendingCallbacks = new ArrayList<>();

    private Locraw() {}

    public static synchronized Locraw get() {
        if (instance == null) {
            instance = new Locraw();
        }
        return instance;
    }

    public interface LocrawCallback {
        void onReceived(String gameType, String mode);
        void onTimeout();
    }

    public void request(LocrawCallback callback) {
        if (callback == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            callback.onTimeout();
            return;
        }

        if (awaitingLocraw) {
            pendingCallbacks.add(callback);
            return;
        }

        pendingCallbacks.add(callback);
        awaitingLocraw = true;
        locrawTimeout = 0;
        mc.thePlayer.sendChatMessage("/locraw");
    }

    @Subscribe
    public void onChat(ChatEvent event) {
        if (awaitingLocraw) {
            String message = event.getMessage().getUnformattedText();
            if (message.startsWith("{") && message.endsWith("}")) {
                try {
                    JsonObject json = new JsonParser().parse(message).getAsJsonObject();
                    gameType = json.has("gametype") ? json.get("gametype").getAsString() : null;
                    mode = json.has("mode") ? json.get("mode").getAsString() : null;
                    event.setCancelled(true);
                    finish(true);
                } catch (Exception e) {
                    finish(false);
                }
            }
        }
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (awaitingLocraw) {
            locrawTimeout++;
            if (locrawTimeout > 100) {
                finish(false);
            }
        }
    }

    private void finish(boolean received) {
        awaitingLocraw = false;
        locrawTimeout = 0;
        if (pendingCallbacks.isEmpty()) {
            return;
        }

        List<LocrawCallback> callbacks = new ArrayList<>(pendingCallbacks);
        pendingCallbacks.clear();
        for (LocrawCallback callback : callbacks) {
            try {
                if (received) {
                    callback.onReceived(gameType, mode);
                } else {
                    callback.onTimeout();
                }
            } catch (Exception e) {
                Debug.error("Locraw callback failed: " + e);
            }
        }
    }

    public String game() {
        return gameType;
    }

    public String mode() {
        return mode;
    }
}
