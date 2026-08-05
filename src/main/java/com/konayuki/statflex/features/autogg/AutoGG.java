package com.konayuki.statflex.features.autogg;

import com.konayuki.statflex.utils.Color;
import com.konayuki.statflex.utils.Debug;
import com.konayuki.statflex.utils.Messages;
import com.konayuki.statflex.utils.Setting;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.Text;

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
    private static final String PREFIX =
            Color.GRAY + "[" + Color.RED + "S" + Color.GRAY + "] ";

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<Pattern> triggers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> gg = Collections.synchronizedList(new ArrayList<>());

    private volatile int tickDelay = 0;
    private volatile boolean sending = false;
    private volatile int sendIndex = 0;

    public AutoGG() {
        Setting.load();
        reloadMessages();
    }

    private void reloadMessages() {
        synchronized (gg) {
            gg.clear();
            String[] saved = Setting.getInstance().gg;
            if (saved != null) {
                Collections.addAll(gg, saved);
            } else {
                Chat.send(PREFIX + "Failed to load AutoGG messages.");
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
        String msg = Text.strip(rawMessage);

        synchronized (triggers) {
            for (Pattern pattern : triggers) {
                if (pattern.matcher(msg).find()) {
                    Debug.log("Trigger matched: " + msg);
                    Setting.load();
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
                        Setting.getInstance().gg = gg.toArray(new String[0]);
                        Setting.save();
                        Chat.send(PREFIX + "Removed message.");
                    } else {
                        Chat.send(PREFIX + "Selected message does not exist.");
                    }
                } catch (NumberFormatException e) {
                    Chat.send(Messages.UNEXPECTED_ERROR);
                }
            } else {
                String msg = String.join(" ", args);
                gg.add(msg);
                Setting.getInstance().gg = gg.toArray(new String[0]);
                Setting.save();
                Chat.send(PREFIX + "Added message: " + Color.YELLOW + msg);
            }
        }
    }

    public void showMessages() {
        Chat.send(PREFIX + "Current AutoGG messages:");
        synchronized (gg) {
            if (gg.isEmpty()) {
                ChatComponentText empty = new ChatComponentText(
                        Color.GRAY + "There's no messages for now. Click the button below to add!");
                mc.thePlayer.addChatMessage(empty);
            } else {
                for (int i = 0; i < gg.size(); i++) {
                    String text = " " + Color.RED + "||" + Color.GRAY + " " + (i + 1) + "."
                            + Color.YELLOW + " " + gg.get(i) + " ";
                    ChatComponentText line = new ChatComponentText(text);

                    ChatComponentText remove = new ChatComponentText(
                            Color.GRAY + "[" + Color.RED + Color.BOLD + "Remove" + Color.GRAY + "]");
                    remove.setChatStyle(new ChatStyle()
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/s autogg remove " + i))
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ChatComponentText("Remove this message."))));
                    line.appendSibling(remove);
                    mc.thePlayer.addChatMessage(line);
                }
            }
        }
        ChatComponentText add = new ChatComponentText(
                Color.GRAY + "[" + Color.GREEN + Color.BOLD + "Add" + Color.GRAY + "]");
        add.setChatStyle(new ChatStyle()
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/s autogg "))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText("Click to add messages."))));
        mc.thePlayer.addChatMessage(add);
    }
}