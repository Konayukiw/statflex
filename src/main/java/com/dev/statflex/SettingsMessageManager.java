package com.dev.statflex;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.HashMap;
import java.util.Map;

public class SettingsMessageManager {

    private static int nextId = 0;

    private static final Map<Integer, IChatComponent> messages = new HashMap<>();

    public static int newMessageId() {
        return nextId++;
    }

    public static void registerMessage(int id, IChatComponent msg) {
        messages.put(id, msg);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().ingameGUI.getChatGUI()
                        .printChatMessageWithOptionalDeletion(msg, id);
            }
        });
    }

    public static void hideMessage(int id) {
        if (messages.containsKey(id)) {
            ChatComponentText empty = new ChatComponentText("");
            int messageId = id;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().ingameGUI.getChatGUI()
                        .printChatMessageWithOptionalDeletion(empty, messageId);
            });
            messages.remove(id);
        }
    }

    public static void updateMessage(int id, IChatComponent msg) {
        messages.put(id, msg);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft.getMinecraft().ingameGUI.getChatGUI()
                    .printChatMessageWithOptionalDeletion(msg, id);
        });
    }
}
