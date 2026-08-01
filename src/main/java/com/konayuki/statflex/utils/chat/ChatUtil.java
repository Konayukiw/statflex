package com.konayuki.statflex.utils.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.HashMap;
import java.util.Map;

public final class ChatUtil {
    private static int nextId = 0;
    private static final Map<Integer, IChatComponent> messages = new HashMap<Integer, IChatComponent>();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private ChatUtil() {
    }

    public static int newMessageId() {
        return nextId++;
    }

    public static void registerMessage(int id, IChatComponent message) {
        messages.put(Integer.valueOf(id), message);
        Chat.runOnClientThread(() -> {
            if (mc.thePlayer != null) {
                mc.ingameGUI.getChatGUI()
                        .printChatMessageWithOptionalDeletion(message, id);
            }
        });
    }

    public static void hideMessage(int id) {
        if (messages.containsKey(Integer.valueOf(id))) {
            ChatComponentText empty = new ChatComponentText("");
            int messageId = id;
            Chat.runOnClientThread(() -> {
                mc.ingameGUI.getChatGUI()
                        .printChatMessageWithOptionalDeletion(empty, messageId);
            });
            messages.remove(Integer.valueOf(id));
        }
    }

    public static void updateMessage(int id, IChatComponent message) {
        messages.put(Integer.valueOf(id), message);
        Chat.runOnClientThread(() -> {
            mc.ingameGUI.getChatGUI()
                    .printChatMessageWithOptionalDeletion(message, id);
        });
    }
}