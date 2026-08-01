package com.konayuki.statflex.utils.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public final class Chat {
    private Chat() {
    }

    public static void send(String message) {
        runOnClientThread(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(message));
            }
        });
    }

    public static void send(IChatComponent component) {
        runOnClientThread(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(component);
            }
        });
    }

    public static void sendCommand(String command) {
        runOnClientThread(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage(command);
            }
        });
    }

    public static void runOnClientThread(Runnable task) {
        Minecraft.getMinecraft().addScheduledTask(task);
    }
}