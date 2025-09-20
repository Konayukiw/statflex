package com.dev.statflex;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class AutoGG {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<Pattern> triggers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> autoMessages = Collections.synchronizedList(new ArrayList<>());

    private volatile int tickDelay = 0;
    private volatile boolean sending = false;
    private volatile int sendIndex = 0;

    public AutoGG() {
        SettingsManager.load();
        reloadMessages();
        System.out.println("[S] Set AutoGG messages: " + autoMessages);
    }

    private void reloadMessages() {
        synchronized (autoMessages) {
            autoMessages.clear();
            String[] saved = SettingsManager.getInstance().autoGGMessages;
            if (saved != null) {
                Collections.addAll(autoMessages, saved);
            } else {
                sendChat("§7[§cS§7] Failed to load AutoGG messages.");
            }
            System.out.println("[S] Reloaded autoMessages: " + autoMessages);
        }
    }

    @SubscribeEvent
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        loadHypixelTriggers();
    }

    private void loadHypixelTriggers() {
        try (InputStream is = AutoGG.class.getResourceAsStream("/regex_triggers_3.json");
                InputStreamReader reader = new InputStreamReader(is)) {
            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(reader).getAsJsonObject();
            JsonArray servers = root.getAsJsonArray("servers");
            for (JsonElement serverElem : servers) {
                JsonObject server = serverElem.getAsJsonObject();
                String name = server.get("name").getAsString();
                if (!name.toLowerCase().contains("hypixel")) {
                    continue;
                }
                JsonArray triggerArr = server.getAsJsonArray("triggers");
                for (JsonElement triggerElem : triggerArr) {
                    JsonObject trigger = triggerElem.getAsJsonObject();
                    if (trigger.get("type").getAsInt() == 0) {
                        String regex = trigger.get("pattern").getAsString();
                        triggers.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                    }
                }
            }
            System.out.println("[S] Loaded " + triggers.size() + " Hypixel triggers.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[S] Failed to load Hypixel triggers.");
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0)
            return;
        if (sending)
            return;

        String rawMessage = event.message.getUnformattedText();
        String msg = stripColors(rawMessage);

        synchronized (triggers) {
            for (Pattern pattern : triggers) {
                if (pattern.matcher(msg).find()) {
                    System.out.println("[S] Trigger matched: " + msg);
                    SettingsManager.load();
                    reloadMessages();
                    synchronized (autoMessages) {
                        if (!autoMessages.isEmpty()) {
                            sending = true;
                            sendIndex = 0;
                            System.out.println("[S] Starting AutoGG sending: " + autoMessages);
                        }
                    }
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null)
            return;

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        if (!sending)
            return;

        synchronized (autoMessages) {
            if (sendIndex < autoMessages.size()) {
                String message = autoMessages.get(sendIndex);
                mc.thePlayer.sendChatMessage("/ac " + message);
                System.out.println("[S] Sending: /ac " + message);
                sendIndex++;
                tickDelay = 2;
            } else {
                sending = false;
                System.out.println("[S] Finished AutoGG sending cycle.");
            }
        }
    }

    public void handleCommand(String[] args) {
        synchronized (autoMessages) {
            if (args.length == 0) {
                showMessages();
            } else if (args[0].equalsIgnoreCase("remove") && args.length == 2) {
                try {
                    int idx = Integer.parseInt(args[1]);
                    if (idx >= 0 && idx < autoMessages.size()) {
                        autoMessages.remove(idx);
                        SettingsManager.getInstance().autoGGMessages = autoMessages.toArray(new String[0]);
                        SettingsManager.save();
                        sendChat("§7[§cS§7] Removed message.");
                    } else {
                        sendChat("§7[§cS§7] Invalid index.");
                    }
                } catch (NumberFormatException e) {
                    sendChat("§7[§cS§7] Index must be a number.");
                }
            } else {
                String msg = String.join(" ", args);
                autoMessages.add(msg);
                SettingsManager.getInstance().autoGGMessages = autoMessages.toArray(new String[0]);
                SettingsManager.save();
                sendChat("§7[§cS§7] Added message: §e" + msg);
            }
            System.out.println("[S] autoMessages after command: " + autoMessages);
        }
    }

    public void showMessages() {
        sendChat("§7[§cS§7] Current AutoGG messages:");
        synchronized (autoMessages) {
            if (autoMessages.isEmpty()) {
                ChatComponentText empty = new ChatComponentText(
                        "§7There's no messages for now. Click the button below to add!");
                mc.thePlayer.addChatMessage(empty);
            } else {
                for (int i = 0; i < autoMessages.size(); i++) {
                    String text = " §c||§7 " + (i + 1) + ".§e " + autoMessages.get(i) + " ";
                    ChatComponentText line = new ChatComponentText(text);

                    ChatComponentText remove = new ChatComponentText("§7[§c§lRemove§7]");
                    remove.setChatStyle(new ChatStyle()
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/s autogg remove " + i))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ChatComponentText("Remove this message."))));
                    line.appendSibling(remove);
                    mc.thePlayer.addChatMessage(line);
                }
                System.out.println("[S] Showing autoMessages: " + autoMessages);
            }
        }
        ChatComponentText add = new ChatComponentText("§7[§a§lAdd§7]");
        add.setChatStyle(new ChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/s autogg "))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Click to add messages."))));
        mc.thePlayer.addChatMessage(add);
    }

    private static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
    }

    private String stripColors(String input) {
        return input.replaceAll("§.", "");
    }
}