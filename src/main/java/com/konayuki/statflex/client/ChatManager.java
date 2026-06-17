package com.konayuki.statflex.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public final class ChatManager {
    private ChatManager() {
    }

    public static void send(String message) {
        runOnClientThread(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
            }
        });
    }

    public static void send(IChatComponent component) {
        runOnClientThread(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(component);
            }
        });
    }

    public static void sendCommand(String command) {
        runOnClientThread(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
            }
        });
    }

    public static void runOnClientThread(Runnable task) {
        Minecraft.getMinecraft().addScheduledTask(task);
    }
}
