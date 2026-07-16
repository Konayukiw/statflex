package com.konayuki.statflex.utils.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.konayuki.statflex.utils.Debug;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Locraw {
    private static Locraw instance;

    private boolean awaitingLocraw = false;
    private String gameType = null;
    private String mode = null;
    private int locrawTimeout = 0;
    private LocrawCallback pendingCallback = null;

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
                awaitingLocraw = false;
                locrawTimeout = 0;
                if (pendingCallback != null) {
                    pendingCallback.onLocrawTimeout();
                    pendingCallback = null;
                }
                Debug.log("[S] Locraw timeout");
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
                    Debug.log("[S] Game=" + gameType + ", Mode=" + mode);
                    event.setCanceled(true);
                    awaitingLocraw = false;
                    locrawTimeout = 0;

                    if (pendingCallback != null) {
                        pendingCallback.onLocrawReceived(gameType, mode);
                        pendingCallback = null;
                    }
                } catch (Exception e) {
                    Debug.log("[S] Failed to parse locraw response: " + e.getMessage());
                    awaitingLocraw = false;
                    locrawTimeout = 0;
                    if (pendingCallback != null) {
                        pendingCallback.onLocrawTimeout();
                        pendingCallback = null;
                    }
                }
            }
        }
    }

    public void requestLocraw(LocrawCallback callback) {
        if (awaitingLocraw) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            callback.onLocrawTimeout();
            return;
        }

        pendingCallback = callback;
        awaitingLocraw = true;
        locrawTimeout = 0;
        mc.thePlayer.sendChatMessage("/locraw");
    }

    public String getCurrentGameType() {
        return gameType;
    }

    public String getCurrentMode() {
        return mode;
    }
}

