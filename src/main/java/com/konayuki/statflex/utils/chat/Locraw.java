package com.konayuki.statflex.utils.chat;

import com.konayuki.statflex.utils.Debug;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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

    public static synchronized Locraw getInstance() {
        if (instance == null) {
            instance = new Locraw();
        }
        return instance;
    }

    public interface LocrawCallback {
        void onLocrawReceived(String gameType, String mode);
        void onLocrawTimeout();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (awaitingLocraw) {
            locrawTimeout++;
            if (locrawTimeout > 100) {
                finish(false);
            }
        }
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (awaitingLocraw) {
            String message = event.message.getUnformattedText();
            if (message.startsWith("{") && message.endsWith("}")) {
                try {
                    JsonObject json = new JsonParser().parse(message).getAsJsonObject();
                    gameType = json.has("gametype") ? json.get("gametype").getAsString() : null;
                    mode = json.has("mode") ? json.get("mode").getAsString() : null;
                    event.setCanceled(true);
                    finish(true);
                } catch (Exception e) {
                    finish(false);
                }
            }
        }
    }

    public void sendLocraw(LocrawCallback callback) {
        if (callback == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            callback.onLocrawTimeout();
            return;
        }

        // A request is already out for the same answer: ride along with it rather than
        // drop the caller, which would leave it waiting on a callback that never comes.
        if (awaitingLocraw) {
            pendingCallbacks.add(callback);
            return;
        }

        pendingCallbacks.add(callback);
        awaitingLocraw = true;
        locrawTimeout = 0;
        mc.thePlayer.sendChatMessage("/locraw");
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
                    callback.onLocrawReceived(gameType, mode);
                } else {
                    callback.onLocrawTimeout();
                }
            } catch (Exception e) {
                Debug.error("Locraw callback failed: " + e);
            }
        }
    }

    public String getCurrentGameType() {
        return gameType;
    }
    public String getCurrentMode() {
        return mode;
    }
}