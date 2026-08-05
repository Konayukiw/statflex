package com.konayuki.statflex.features.autogg;

import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.Settings;
import com.konayuki.statflex.utils.packet.Messages;
import com.konayuki.statflex.utils.Strip;

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
    private final List<String> gg = Collections.synchronizedList(new ArrayList<>());

    private volatile int tickDelay = 0;
    private volatile boolean sending = false;
    private volatile int sendIndex = 0;

    public AutoGG() {
        Settings.load();
        reloadMessages();
    }

    private void reloadMessages() {
        synchronized (gg) {
            gg.clear();
            String[] saved = Settings.getInstance().gg;
            if (saved != null) {
                Collections.addAll(gg, saved);
            } else {
                Chat.send("§7[§cS§7] Failed to load AutoGG messages.");
            }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (event.type != 0)
            return;
        if (sending)
            return;

        String rawMessage = event.message.getUnformattedText();
        String msg = Strip.stripColor(rawMessage);

        synchronized (triggers) {
            for (Pattern pattern : triggers) {
                if (pattern.matcher(msg).find()) {
                    Debug.log("Trigger matched: " + msg);
                    Settings.load();
                    reloadMessages();
                    synchronized (gg) {
                        if (!gg.isEmpty()) {
                            sending = true;
                            sendIndex = 0;
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

        synchronized (gg) {
            if (sendIndex < gg.size()) {
                String message = gg.get(sendIndex);
                mc.thePlayer.sendChatMessage("/ac " + message);
                sendIndex++;
                tickDelay = 2;
            } else {
                sending = false;
            }
        }
    }

    public void handleCommand(String[] args) {
        synchronized (gg) {
            if (args.length == 0) {
                showMessages();
            } else if (args[0].equalsIgnoreCase("remove") && args.length == 2) {
                try {
                    int idx = Integer.parseInt(args[1]);
                    if (idx >= 0 && idx < gg.size()) {
                        gg.remove(idx);
                        Settings.getInstance().gg = gg.toArray(new String[0]);
                        Settings.save();
                        Chat.send("§7[§cS§7] Removed message.");
                    } else {
                        Chat.send("§7[§cS§7] Selected message does not exist.");
                    }
                } catch (NumberFormatException e) {
                    Chat.send(Messages.UNEXPECTED_ERROR);
                }
            } else {
                String msg = String.join(" ", args);
                gg.add(msg);
                Settings.getInstance().gg = gg.toArray(new String[0]);
                Settings.save();
                Chat.send("§7[§cS§7] Added message: §e" + msg);
            }
        }
    }

    public void showMessages() {
        Chat.send("§7[§cS§7] Current AutoGG messages:");
        synchronized (gg) {
            if (gg.isEmpty()) {
                ChatComponentText empty = new ChatComponentText(
                        "§7There's no messages for now. Click the button below to add!");
                mc.thePlayer.addChatMessage(empty);
            } else {
                for (int i = 0; i < gg.size(); i++) {
                    String text = " §c||§7 " + (i + 1) + ".§e " + gg.get(i) + " ";
                    ChatComponentText line = new ChatComponentText(text);

                    ChatComponentText remove = new ChatComponentText("§7[§c§lRemove§7]");
                    remove.setChatStyle(new ChatStyle()
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/s autogg remove " + i))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ChatComponentText("Remove this message."))));
                    line.appendSibling(remove);
                    mc.thePlayer.addChatMessage(line);
                }
            }
        }
        ChatComponentText add = new ChatComponentText("§7[§a§lAdd§7]");
        add.setChatStyle(new ChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/s autogg "))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Click to add messages."))));
        mc.thePlayer.addChatMessage(add);
    }
}