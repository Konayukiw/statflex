package com.konayuki.statflex.utils;

import com.konayuki.statflex.features.autogg.AutoGG;
import com.konayuki.statflex.features.skin.Skin;
import com.konayuki.statflex.features.namehistory.NameHistory;
import com.konayuki.statflex.features.bedwars.Bedwars;
import com.konayuki.statflex.features.duels.Duels;
import com.konayuki.statflex.features.duels.DuelsUpdated;
import com.konayuki.statflex.features.skywars.Skywars;
import com.konayuki.statflex.gui.Gui;

import com.konayuki.statflex.utils.api.HypixelApiUtil;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.chat.ChatUtil;
import com.konayuki.statflex.events.EventBus;
import com.konayuki.statflex.events.SendChatEvent;
import com.konayuki.statflex.events.Subscribe;
import com.konayuki.statflex.events.TickEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Commands {
    private static final String BULLET = Color.RED + " || " + Color.GRAY;
    private final List<String> aliases = Arrays.asList("s");
    private static final AutoGG AUTO_GG_HANDLER = new AutoGG();
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean registered;
    private static int openGuiTicks = -1;
    private static String openGuiTab;
    private static final int SETTINGS_CHAT_ID = 99999;

    public static synchronized void register() {
        if (registered) {
            return;
        }

        EventBus.register(new Commands());
        registered = true;
    }

    public void execute(String[] args) {
        if (args.length < 1) {
            open(null);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "api":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String key = args[1];

                HypixelApiUtil.set(key);
                Setting.get().apiKey = key;
                Setting.save();
                Chat.send(Messages.API_SET);
                break;

            case "flag":
            case "interval":
                flag(args);
                break;

            case "bw":
            case "bedwars":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String bwName = args[1];
                String bwMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                Bedwars.stats(bwName, bwMode);
                break;

            case "sw":
            case "sky":
            case "skywars":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String swName = args[1];
                String swMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                Skywars.stats(swName, swMode);
                break;

            case "duels":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String duelsName = args[1];
                String duelsMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                if (Toggle.duelsUpdated) {
                    DuelsUpdated.stats(duelsName, duelsMode);
                } else {
                    Duels.stats(duelsName, duelsMode);
                }
                break;

            case "duelsupdate":
                Toggle.flipNewDuels(false);
                break;

            case "list":
                Toggle.flipBwList(false);
                Toggle.flipSwList(false);
                break;

            case "auto":
                Toggle.flipAuto(false);
                break;

            case "nh":
            case "namehistory":
                if (args.length >= 2) {
                    if (args[0].equalsIgnoreCase("nh") || args[0].equalsIgnoreCase("namehistory")) {
                        String targetName = args[1];
                        NameHistory.show(targetName);
                        return;
                    }
                }
                break;

            case "skin":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String skinPlayerName = args[1];
                boolean useNpcSkin = false;

                if (args.length >= 3 && args[2].equalsIgnoreCase("-npcskin")) {
                    useNpcSkin = true;
                }
                Skin.save(skinPlayerName, useNpcSkin);
                break;

            case "autogg":
                if (args.length == 1) {
                    AUTO_GG_HANDLER.show();
                } else {
                    String[] messageArgs = Arrays.copyOfRange(args, 1, args.length);
                    AUTO_GG_HANDLER.command(messageArgs);
                }
                break;

            case "secure":
            case "secureconnection":
                Toggle.flipInsecure(false);
                break;

            case "denick":
            case "denickEnabled":
                Toggle.flipDenick(false);
                break;

            case "rpc":
            case "discordrpc":
                Toggle.flipRpc(false);
                break;

            case "keepwho":
                Toggle.flipKeepWho(false);
                break;

            case "warn":
                if (args.length < 2) {
                    Chat.send(Messages.PREFIX + "Usage: /s warn [Level] [FKDR] / /s warn [Level] / /s warn [FKDR]");
                    return;
                }
                try {
                    Setting setting = Setting.get();

                    if (args.length == 2) {
                        if (args[1].contains(".")) {
                            setting.warnLevel = 0;
                            setting.warnFKDR = Double.parseDouble(args[1]);
                            Chat.send(Messages.PREFIX + "Players higher than " + Color.YELLOW + Color.BOLD
                                    + setting.warnFKDR + " FKDR " + Color.GRAY + "will be warned.");
                        } else {
                            setting.warnLevel = Integer.parseInt(args[1]);
                            setting.warnFKDR = 0;
                            Chat.send(Messages.PREFIX + "Players higher than " + Color.YELLOW + Color.BOLD
                                    + "✫" + setting.warnLevel + Color.GRAY + " will be warned.");
                        }
                    } else {
                        setting.warnLevel = Integer.parseInt(args[1]);
                        setting.warnFKDR = Double.parseDouble(args[2]);
                        Chat.send(Messages.PREFIX + "Players higher than " + Color.YELLOW + Color.BOLD
                                + "✫" + setting.warnLevel + Color.GRAY + ", " + Color.YELLOW + Color.BOLD
                                + setting.warnFKDR + " FKDR " + Color.GRAY + "will be warned.");
                    }

                    Setting.save();
                } catch (NumberFormatException e) {
                    Chat.send(Messages.PREFIX + "Invalid number format.");
                }
                break;

            case "update":
                open("Update");
                break;

            case "dir":
                if (args.length >= 2) {

                    String rawPath = args[1];
                    File dir = new File(rawPath);

                    if (!dir.isAbsolute()) {
                        Chat.send(Messages.PREFIX + "No relative paths are allowed.");
                        break;
                    }

                    try {
                        dir = dir.getCanonicalFile();
                    } catch (IOException e) {
                        Chat.send(Messages.PREFIX + "Invalid path.");
                        break;
                    }

                    if (!dir.exists() && !dir.mkdirs()) {
                        Chat.send(Messages.PREFIX + "Failed to create directory.");
                        Chat.send(Messages.PREFIX + "statflex may fail configure files under C:/Windows or C:/Program Files.");
                        break;
                    }

                    if (!dir.isDirectory()) {
                        Chat.send(Messages.PREFIX + "Select a directory.");
                        break;
                    }

                    Setting.get().setDir(dir);

                    Chat.send(Messages.PREFIX + "Skin save directory set to:" + Color.YELLOW + dir.getAbsolutePath());
                    break;

                } else {
                    Chat.send(Messages.PREFIX + "Usage: /s dir " + Color.YELLOW + "[Path]" + Color.GRAY + " to determine the path.");
                    break;
                }

            case "toggle":
                if (args.length >= 2) {
                    String setting = args[1].toLowerCase();
                    switch (setting) {
                        case "liststats":
                            Toggle.flipBwList(true);
                            break;
                        case "autoduels":
                            Toggle.flipAuto(true);
                            break;
                        case "duelsupdate":
                            Toggle.flipNewDuels(true);
                            break;
                        case "denick":
                        case "denickEnabled":
                            Toggle.flipDenick(true);
                            break;
                        case "autogg":
                            break;
                        case "secure":
                            Toggle.flipInsecure(true);
                            break;
                        case "keepwho":
                            Toggle.flipKeepWho(true);
                            break;
                        default:
                            Chat.send(Messages.USAGE);
                            return;
                    }
                    settings();
                } else {
                    Chat.send(Messages.INVALID_COMMAND);
                }
                break;

            case "settings":
                settings();
                break;

            case "help":
                Chat.send(Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY + "] " + Color.GRAY + "Available commands:");
                Chat.send(BULLET + "/s api " + Color.AQUA + "[API Key] " + Color.DARK_GRAY + ": " + Color.GRAY + "Sets Server API Key to enable stats viewer.");
                Chat.send(BULLET + "- You must get API Key from " + Color.YELLOW + "https://developer.hypixel.net");
                Chat.send(BULLET + "/s flag " + Color.DARK_GRAY + ": " + Color.GRAY + "Sets Anticheat flag interval. It's up to you.");
                Chat.send(BULLET + "/s bw " + Color.YELLOW + "[Player] -[Mode] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Bedwars stats in-game.");
                Chat.send(BULLET + "/s sw " + Color.YELLOW + "[Player] -[Mode] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Skywars stats in-game.");
                Chat.send(BULLET + "/s duels " + Color.YELLOW + "[Player] -[Mode] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Duels stats in-game.");
                Chat.send(BULLET + "/s nh " + Color.YELLOW + "[Player] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Name History.");
                Chat.send(BULLET + "/s autogg " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows current AutoGG messages.");
                Chat.send(BULLET + "/s autogg " + Color.YELLOW + "[Messages] " + Color.DARK_GRAY + ": " + Color.GRAY + "Add new AutoGG message.");
                Chat.send(BULLET + "- Keep it under 9 messages or get blocked for spamming.");
                Chat.send(BULLET + "/s list " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggle whether the stats list is displayed with /who.");
                Chat.send(BULLET + "/s auto " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggle auto stats viewer for Duels.");
                Chat.send(BULLET + "/s denick " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggle Denicker which can denick original skin users.");
                Chat.send(BULLET + "- It's possibly bannable, use at your own risk.");
                Chat.send(BULLET + "/s keepwho " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggle whether the original /who message remains visible.");
                Chat.send(BULLET + "/s skin " + Color.YELLOW + "[Player] " + Color.DARK_GRAY + ": " + Color.GRAY + "Download their skin locally.");
                Chat.send(BULLET + "- Add -npcSkin to force saving NPC or Nick Skin if they have existing username.");
                Chat.send(BULLET + "/s dir " + Color.YELLOW + "[Path] " + Color.DARK_GRAY + ": " + Color.GRAY + "Determines the directory to save skin files.");
                Chat.send(BULLET + "/s add " + Color.YELLOW + "[Player] [Reason] " + Color.DARK_GRAY + ": " + Color.GRAY + "Reports cheaters to share and notify when you queued them.");
                Chat.send(BULLET + "/s settings " + Color.DARK_GRAY + ": " + Color.GRAY + "Opens togglable settings");
                Chat.send(BULLET + "/s secure " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggle secure connections.");
                Chat.send(BULLET + "- This should be disabled if you have errors while getting stats.");
                Chat.send(BULLET + "- Usually, disabling this is not recommended as it can be insecure.");
                Chat.send(" ");
                Chat.send(BULLET + "/s update " + Color.DARK_GRAY + ": " + Color.GRAY + "Opens the Update tab in the settings GUI.");
                Chat.send(BULLET + "/s help " + Color.DARK_GRAY + ": " + Color.GRAY + "Opens this help");
                Chat.send(BULLET + "If you don't understand well, watch introduction video!");
                Chat.send(BULLET + " " + Color.YELLOW + "https://www.youtube.com/watch?v=(UPLOAD_SOON)");
                break;

            default:
                Chat.send(Messages.INVALID_COMMAND);
        }
    }

    private void flag(String[] args) {
        if (args.length < 2) {
            Chat.send(Messages.USAGE);
            Chat.send(BULLET + "Current value: " + Color.YELLOW + Color.BOLD
                    + Setting.get().flag() + Color.GRAY + "s");
            return;
        }

        try {
            double value = Double.parseDouble(args[1]);
            if (value < 0) value = 0;
            if (value > 20) value = 20;

            Setting.get().setFlag(value);

            Chat.send(BULLET + "Set flag interval to " + Color.YELLOW + Color.BOLD + value + Color.GRAY + "s");
        } catch (NumberFormatException e) {
            Chat.send(BULLET + "Invalid value. Min: 0, Max: 20");
        }
    }

    private static void settings() {
        try {
            IChatComponent root = new ChatComponentText(
                    Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY + "] " + Color.GRAY + "Setting:\n");

            String enabled = Color.AQUA.toString() + Color.BOLD + "Enabled";
            String disabled = Color.RED.toString() + Color.BOLD + "Disabled";

            String[][] settings = {
                    { "Denick", Toggle.denick ? enabled : disabled, "denickEnabled",
                            "Toggle Denick " + enabled + " / " + disabled + ". \n" + Color.YELLOW
                                    + "Do not use denickEnabled if you want to be fully legit. This may cause of a Server Ban." },
                    { "Bedwars Stats List", Toggle.listStats ? enabled : disabled, "listStats",
                            "Toggle Auto-Stats List with /who. \n" + Color.YELLOW
                                    + "With this disabled, you can see original /who list." },
                    { "Auto Duels Stats", Toggle.autoStats ? enabled : disabled, "autoDuels",
                            "Toggle Auto Duels Stats. \n" + Color.YELLOW
                                    + "You can get enemy stats automatically" },
                    { "Updated Duels Titles", Toggle.duelsUpdated ? enabled : disabled, "duelsUpdated",
                            "Toggle New Duels Titles. \n" + Color.YELLOW
                                    + "With this enabled, Duels Title can be shown with updated schemes." },
                    { "Secure Connection", !Toggle.ignoreCertificates ? enabled : disabled, "secure",
                            Color.RED.toString() + Color.BOLD + "Do NOT Enable this! " + Color.YELLOW
                                    + "Only use this to avoid fetching errors. \n" + Color.YELLOW
                                    + "This lets you allow all certificates." },
                    { "Keep Original /who", Toggle.keepWho ? enabled : disabled, "keepwho",
                            "Keep original /who output visible while Bedwars Stats List Enabled." }
            };

            for (String[] s : settings) {
                IChatComponent line = new ChatComponentText(BULLET + s[0] + ": " + s[1] + "\n");

                line.getChatStyle().setChatClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/s toggle " + s[2]));

                line.getChatStyle().setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(Color.YELLOW + s[3])));

                root.appendSibling(line);
            }

            ChatUtil.register(SETTINGS_CHAT_ID, root);

        } catch (Exception e) {
            e.printStackTrace();
            Chat.send(Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY + "] " + Color.GRAY + "Failed to open settings.");
        }
    }

    private static void open(String tabId) {
        openGuiTab = tabId;
        openGuiTicks = 1;
    }

    @Subscribe
    public void onTick(TickEvent event) {
        if (openGuiTicks < 0) {
            return;
        }
        openGuiTicks--;
        if (openGuiTicks != 0) {
            return;
        }
        String tab = openGuiTab;
        openGuiTab = null;
        mc.displayGuiScreen(new Gui(tab));
    }

    public static void sync(Setting setting) {
        Toggle.sync(setting);
    }

    public static String commandName() {
        return "s";
    }

    public List<String> aliases() {
        return aliases;
    }

    @Subscribe
    public void onSendChat(SendChatEvent event) {
        String message = event.getMessage();
        if (message == null) {
            return;
        }

        String[] args = parseCommand(message);
        if (args == null) {
            return;
        }

        event.setCancelled(true);
        execute(args);
    }

    private static String[] parseCommand(String message) {
        String lower = message.toLowerCase();
        String prefix = null;
        if (lower.equals("/s") || lower.startsWith("/s ")) {
            prefix = "/s";
        } else if (lower.equals("/statflex") || lower.startsWith("/statflex ")) {
            prefix = "/statflex";
        }
        if (prefix == null) {
            return null;
        }

        String rest = message.substring(prefix.length()).trim();
        if (rest.isEmpty()) {
            return new String[0];
        }
        return rest.split(" ");
    }
}
