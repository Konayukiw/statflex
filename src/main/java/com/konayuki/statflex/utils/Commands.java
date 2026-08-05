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
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Commands implements ICommand {

    /** Leading bullet used by /s help and a few status lines. */
    private static final String BULLET = Color.RED + " || " + Color.GRAY;

    private final List<String> aliases = Arrays.asList("s");

    private static final AutoGG AUTO_GG_HANDLER = new AutoGG();
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean registered;
    private static int openGuiTicks = -1;
    private static String openGuiTab;

    public static void syncFromSettings(Settings settings) {
        Toggles.syncFromSettings(settings);
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        Commands instance = new Commands();
        ClientCommandHandler.instance.registerCommand(instance);
        MinecraftForge.EVENT_BUS.register(instance);
        registered = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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

    private static void openGui(String tabId) {
        openGuiTab = tabId;
        openGuiTicks = 1;
    }

    @Override
    public String getCommandName() {
        return "s";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Messages.USAGE;
    }

    @Override
    public List<String> getCommandAliases() {
        return aliases;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            openGui(null);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "api":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String key = args[1];

                HypixelApiUtil.setApiKey(key);
                Settings.getInstance().apiKey = key;
                Settings.save();
                Chat.send(Messages.API_SET);
                break;

            case "flag":
            case "interval":
                handleFlagCommand(sender, args);
                break;

            case "bw":
            case "bedwars":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String bwName = args[1];
                String bwMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                Bedwars.fetchStats(bwName, bwMode);
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
                Skywars.fetchStats(swName, swMode);
                break;

            case "duels":
                if (args.length < 2) {
                    Chat.send(Messages.USAGE);
                    return;
                }
                String duelsName = args[1];
                String duelsMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                if (Toggles.duelsUpdated) {
                    DuelsUpdated.fetchStats(duelsName, duelsMode);
                } else {
                    Duels.fetchStats(duelsName, duelsMode);
                }
                break;

            case "duelsupdate":
                Toggles.toggleDuelsUpdate(false);
                break;

            case "list":
                Toggles.toggleListStats(false);
                break;

            case "auto":
                Toggles.toggleAutoStats(false);
                break;

            case "nh":
            case "namehistory":
                if (args.length >= 2) {
                    if (args[0].equalsIgnoreCase("nh") || args[0].equalsIgnoreCase("namehistory")) {
                        String targetName = args[1];
                        NameHistory.getNameHistory(targetName);
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
                Skin.savePlayerSkinAsync(skinPlayerName, useNpcSkin);
                break;

            case "autogg":
                if (args.length == 1) {
                    AUTO_GG_HANDLER.showMessages();
                } else {
                    String[] messageArgs = Arrays.copyOfRange(args, 1, args.length);
                    AUTO_GG_HANDLER.handleCommand(messageArgs);
                }
                break;

            case "secure":
            case "secureconnection":
                Toggles.toggleIgnoreCertificates(false);
                break;

            case "denick":
            case "denickEnabled":
                Toggles.toggleDenick(false);
                break;

            case "rpc":
            case "discordrpc":
                Toggles.toggleDiscordRpc(false);
                break;

            case "keepwho":
                Toggles.toggleKeepWho(false);
                break;

            case "warn":
                if (args.length < 2) {
                    Chat.send(Messages.PREFIX + "Usage: /s warn [Level] [FKDR] / /s warn [Level] / /s warn [FKDR]");
                    return;
                }
                try {
                    Settings settings = Settings.getInstance();

                    if (args.length == 2) {
                        if (args[1].contains(".")) {
                            settings.warnLevel = 0;
                            settings.warnFKDR = Double.parseDouble(args[1]);
                            Chat.send(Messages.PREFIX + "Players higher than " + Color.YELLOW + Color.BOLD
                                    + settings.warnFKDR + " FKDR " + Color.GRAY + "will be warned.");
                        } else {
                            settings.warnLevel = Integer.parseInt(args[1]);
                            settings.warnFKDR = 0;
                            Chat.send(Messages.PREFIX + "Players higher than " + Color.YELLOW + Color.BOLD
                                    + "✫" + settings.warnLevel + Color.GRAY + " will be warned.");
                        }
                    } else {
                        settings.warnLevel = Integer.parseInt(args[1]);
                        settings.warnFKDR = Double.parseDouble(args[2]);
                        Chat.send(Messages.PREFIX + "Players higher than " + Color.YELLOW + Color.BOLD
                                + "✫" + settings.warnLevel + Color.GRAY + ", " + Color.YELLOW + Color.BOLD
                                + settings.warnFKDR + " FKDR " + Color.GRAY + "will be warned.");
                    }

                    Settings.save();
                } catch (NumberFormatException e) {
                    Chat.send(Messages.PREFIX + "Invalid number format.");
                }
                break;

            case "update":
                openGui("Update");
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

                    Settings.getInstance().setSkinSaveDir(dir);

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
                            Toggles.toggleListStats(true);
                            break;
                        case "autoduels":
                            Toggles.toggleAutoStats(true);
                            break;
                        case "duelsupdate":
                            Toggles.toggleDuelsUpdate(true);
                            break;
                        case "denick":
                        case "denickEnabled":
                            Toggles.toggleDenick(true);
                            break;
                        case "autogg":
                            break;
                        case "secure":
                            Toggles.toggleIgnoreCertificates(true);
                            break;
                        case "keepwho":
                            Toggles.toggleKeepWho(true);
                            break;
                        default:
                            Chat.send(Messages.USAGE);
                            return;
                    }
                    sendSettings();
                } else {
                    Chat.send(Messages.INVALID_COMMAND);
                }
                break;

            case "settings":
                sendSettings();
                break;

            case "help":
                Chat.send(Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY + "] " + Color.GRAY + "Available commands:");
                Chat.send(BULLET + "/s api " + Color.AQUA + "[API Key] " + Color.DARK_GRAY + ": " + Color.GRAY + "Sets Hypixel API Key to enable stats viewer.");
                Chat.send(BULLET + "- You must get API Key from " + Color.YELLOW + "https://developer.hypixel.net");
                Chat.send(BULLET + "/s flag " + Color.DARK_GRAY + ": " + Color.GRAY + "Sets Anticheat flag interval. It's up to you.");
                Chat.send(BULLET + "/s bw " + Color.YELLOW + "[Player] -[Mode] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Bedwars stats in-game.");
                Chat.send(BULLET + "/s sw " + Color.YELLOW + "[Player] -[Mode] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Skywars stats in-game.");
                Chat.send(BULLET + "/s duels " + Color.YELLOW + "[Player] -[Mode] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Duels stats in-game.");
                Chat.send(BULLET + "/s nh " + Color.YELLOW + "[Player] " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows their Name History.");
                Chat.send(BULLET + "/s autogg " + Color.DARK_GRAY + ": " + Color.GRAY + "Shows current AutoGG messages.");
                Chat.send(BULLET + "/s autogg " + Color.YELLOW + "[Message] " + Color.DARK_GRAY + ": " + Color.GRAY + "Add new AutoGG message.");
                Chat.send(BULLET + "- Keep it under 9 messages or get blocked for spamming.");
                Chat.send(BULLET + "/s list " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggles whether the stats list is displayed with /who.");
                Chat.send(BULLET + "/s auto " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggles auto stats viewer for Duels.");
                Chat.send(BULLET + "/s denick " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggles Denicker which can denick original skin users.");
                Chat.send(BULLET + "- It's possibly bannable, use at your own risk.");
                Chat.send(BULLET + "/s keepwho " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggles whether the original /who message remains visible.");
                Chat.send(BULLET + "/s skin " + Color.YELLOW + "[Player] " + Color.DARK_GRAY + ": " + Color.GRAY + "Download their skin locally.");
                Chat.send(BULLET + "- Add -npcSkin to force saving NPC or Nick Skin if they have existing username.");
                Chat.send(BULLET + "/s dir " + Color.YELLOW + "[Path] " + Color.DARK_GRAY + ": " + Color.GRAY + "Determines the directory to save skin files.");
                Chat.send(BULLET + "/s add " + Color.YELLOW + "[Player] [Reason] " + Color.DARK_GRAY + ": " + Color.GRAY + "Reports cheaters to share and notify when you queued them.");
                Chat.send(BULLET + "/s settings " + Color.DARK_GRAY + ": " + Color.GRAY + "Opens togglable settings");
                Chat.send(BULLET + "/s secure " + Color.DARK_GRAY + ": " + Color.GRAY + "Toggles secure connections.");
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

    private void handleFlagCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            Chat.send(Messages.USAGE);
            Chat.send(BULLET + "Current value: " + Color.YELLOW + Color.BOLD
                    + Settings.getInstance().getFlagInterval() + Color.GRAY + "s");
            return;
        }

        try {
            double value = Double.parseDouble(args[1]);
            if (value < 0) value = 0;
            if (value > 20) value = 20;

            Settings.getInstance().setFlagInterval(value);

            Chat.send(BULLET + "Set flag interval to " + Color.YELLOW + Color.BOLD + value + Color.GRAY + "s");
        } catch (NumberFormatException e) {
            Chat.send(BULLET + "Invalid value. Min: 0, Max: 20");
        }
    }

    private static final int SETTINGS_CHAT_ID = 99999;

    private static void sendSettings() {
        try {
            IChatComponent root = new ChatComponentText(
                    Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY + "] " + Color.GRAY + "Settings:\n");

            String enabled = Color.AQUA.toString() + Color.BOLD + "Enabled";
            String disabled = Color.RED.toString() + Color.BOLD + "Disabled";

            String[][] settings = {
                    { "Denick", Toggles.denick ? enabled : disabled, "denickEnabled",
                            "Toggle Denick " + enabled + " / " + disabled + ". \n" + Color.YELLOW
                                    + "Do not use denickEnabled if you want to be fully legit. This may cause of a Hypixel Ban." },
                    { "Bedwars Stats List", Toggles.listStats ? enabled : disabled, "listStats",
                            "Toggle Auto-Stats List with /who. \n" + Color.YELLOW
                                    + "With this disabled, you can see original /who list." },
                    { "Auto Duels Stats", Toggles.autoStats ? enabled : disabled, "autoDuels",
                            "Toggle Auto Duels Stats. \n" + Color.YELLOW
                                    + "You can get enemy stats automatically" },
                    { "Updated Duels Titles", Toggles.duelsUpdated ? enabled : disabled, "duelsUpdated",
                            "Toggle New Duels Titles. \n" + Color.YELLOW
                                    + "With this enabled, Duels Title can be shown with updated schemes." },
                    { "Secure Connection", !Toggles.ignoreCertificates ? enabled : disabled, "secure",
                            Color.RED.toString() + Color.BOLD + "Do NOT Enable this! " + Color.YELLOW
                                    + "Only use this to avoid fetching errors. \n" + Color.YELLOW
                                    + "This lets you allow all certificates." },
                    { "Keep Original /who", Toggles.keepWho ? enabled : disabled, "keepwho",
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

            ChatUtil.registerMessage(SETTINGS_CHAT_ID, root);

        } catch (Exception e) {
            e.printStackTrace();
            Chat.send(Color.DARK_GRAY + "[" + Color.RED + "S" + Color.DARK_GRAY + "] " + Color.GRAY + "Failed to open settings.");
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return Collections.emptyList();
    }

    @Override
    public int compareTo(ICommand o) {
        return this.getCommandName().compareTo(o.getCommandName());
    }
}
