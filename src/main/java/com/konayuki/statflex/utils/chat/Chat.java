package com.konayuki.statflex.utils.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public final class Chat {
    private Chat() {
    }

    public static void send(String message) {
        run(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(message));
            }
        });
    }

    public static void send(IChatComponent component) {
        run(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(component);
            }
        });
    }

    public static void command(String command) {
        run(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage(command);
            }
        });
    }

    public static void run(Runnable task) {
        Minecraft.getMinecraft().addScheduledTask(task);
    }
}