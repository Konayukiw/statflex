package com.konayuki.statflex.command;

import com.konayuki.statflex.client.ChatManager;
import com.konayuki.statflex.client.ChatOverlayManager;
import com.konayuki.statflex.config.ApiKeyManager;
import com.konayuki.statflex.config.Toggles;
import com.konayuki.statflex.config.Settings;
import com.konayuki.statflex.feature.autogg.AutoGG;
import com.konayuki.statflex.feature.skin.SkinSaver;
import com.konayuki.statflex.system.Messages;
import com.konayuki.statflex.feature.namehistory.NameHistory;
import com.konayuki.statflex.stats.bedwars.BedwarsFetcher;
import com.konayuki.statflex.stats.duels.DuelsFetcher;
import com.konayuki.statflex.stats.duels.DuelsFetcherUpdated;
import com.konayuki.statflex.stats.skywars.SkywarsFetcher;
import com.konayuki.statflex.update.UpdateChecker;
import com.konayuki.statflex.update.UpdateGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Commands implements ICommand {

    private final List<String> aliases = Arrays.asList("s");

    private static final AutoGG AUTO_GG_HANDLER = new AutoGG();
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean registered;

    public static void syncFromSettings(Settings settings) {
        Toggles.syncFromSettings(settings);
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        ClientCommandHandler.instance.registerCommand(new Commands());
        registered = true;
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
            ChatManager.send(Messages.USAGE);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "api":
                if (args.length < 2) {
                    ChatManager.send(Messages.USAGE);
                    return;
                }
                String key = args[1];

                ApiKeyManager.setApiKey(args[1]);
                ApiKeyManager.setApiKey(key);
                Settings.getInstance().apiKey = key;
                Settings.save();
                ChatManager.send(Messages.API_SET);
                break;

            case "flag":
            case "interval":
                handleFlagCommand(sender, args);
                break;

            case "bw":
            case "bedwars":
                if (args.length < 2) {
                    ChatManager.send(Messages.USAGE);
                    return;
                }
                String bwName = args[1];
                String bwMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                BedwarsFetcher.fetchStats(bwName, bwMode);
                break;

            case "sw":
            case "skywars":
                if (args.length < 2) {
                    ChatManager.send(Messages.USAGE);
                    return;
                }
                String swName = args[1];
                String swMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                SkywarsFetcher.fetchStats(swName, swMode);
                break;

            case "duels":
                if (args.length < 2) {
                    ChatManager.send(Messages.USAGE);
                    return;
                }
                String duelsName = args[1];
                String duelsMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                if (Toggles.duelsUpdate) {
                    DuelsFetcherUpdated.fetchStats(duelsName, duelsMode);
                } else {
                    DuelsFetcher.fetchStats(duelsName, duelsMode);
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
                        NameHistory.fetchNameHistory(targetName);
                        return;
                    }
                }
                break;

            case "skin":
                if (args.length < 2) {
                    ChatManager.send(Messages.USAGE);
                    return;
                }
                String skinPlayerName = args[1];
                boolean useNpcSkin = false;

                if (args.length >= 3 && args[2].equalsIgnoreCase("-npcskin")) {
                    useNpcSkin = true;
                }
                SkinSaver.savePlayerSkinAsync(skinPlayerName, useNpcSkin);
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
                Toggles.toggleIgnoreCertificates(false);
                break;

            case "denick":
                Toggles.toggleDenick(false);
                break;

            case "keepwho":
                Toggles.toggleKeepWho(false);
                break;

            case "warn":
                if (args.length < 2) {
                    ChatManager.send("§8[§cS§8]§7 Usage: /s warn [Level] [FKDR] / /s warn [Level] / /s warn [FKDR]");
                    return;
                }
                try {
                    Settings settings = Settings.getInstance();

                    if (args.length == 2) {
                        if (args[1].contains(".")) {
                            settings.warnLevel = 0;
                            settings.warnFKDR = Double.parseDouble(args[1]);
                            ChatManager.send("§8[§cS§8]§7 Players higher than §e§l" + settings.warnFKDR + " FKDR §7will be warned.");
                        } else {
                            settings.warnLevel = Integer.parseInt(args[1]);
                            settings.warnFKDR = 0;
                            ChatManager.send("§8[§cS§8]§7 Players higher than §e§l✫" + settings.warnLevel + "§7 will be warned.");
                        }
                    } else {
                        settings.warnLevel = Integer.parseInt(args[1]);
                        settings.warnFKDR = Double.parseDouble(args[2]);
                        ChatManager.send("§8[§cS§8]§7 Players higher than §e§l✫" + settings.warnLevel + "§7, §e§l" + settings.warnFKDR + " FKDR §7will be warned.");
                    }

                    Settings.save();
                } catch (NumberFormatException e) {
                    ChatManager.send("§8[§cS§8]§7 Invalid number format.");
                }
                break;

            case "update": {
                ChatManager.send("§8[§cS§8]§7 Checking for updates...");

                new Thread(() -> {
                    UpdateChecker.UpdateState state = UpdateChecker.checkNow();

                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        switch (state) {
                            case UP_TO_DATE:
                                ChatManager.send("§8[§cS§8]§7 statflex is up-to-date!");
                                break;

                            case UPDATE_AVAILABLE:
                                ChatManager.send("§8[§cS§8]§7 Update is available.");
                                Minecraft.getMinecraft().displayGuiScreen(
                                        new UpdateGuiScreen()
                                );
                                break;

                            case ERROR:
                                ChatManager.send("§8[§cS§8]§7 Failed to check updates.");
                                break;
                        }
                    });
                }, "statflex-updater").start();

                break;
            }

            case "dir":
                if (args.length >= 2) {

                    String rawPath = args[1];
                    File dir = new File(rawPath);

                    if (!dir.isAbsolute()) {
                        ChatManager.send("§8[§cS§8]§7 No relative paths are allowed.");
                        break;
                    }

                    try {
                        dir = dir.getCanonicalFile();
                    } catch (IOException e) {
                        ChatManager.send("§8[§cS§8]§7 Invalid path.");
                        break;
                    }

                    if (!dir.exists() && !dir.mkdirs()) {
                        ChatManager.send("§8[§cS§8]§7 Failed to create directory.");
                        ChatManager.send("§8[§cS§8]§7 statflex may fail configure files under C:/Windows or C:/Program Files.");
                        break;
                    }

                    if (!dir.isDirectory()) {
                        ChatManager.send("§8[§cS§8]§7 Select a directory.");
                        break;
                    }

                    Settings.getInstance().setSkinSaveDir(dir);

                    ChatManager.send("§8[§cS§8]§7 Skin save directory set to:" + "§e" + dir.getAbsolutePath());
                    break;

                } else {
                    ChatManager.send("§8[§cS§8]§7 Usage: /s dir §e[Path]§7 to determine the path.");
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
                            Toggles.toggleDenick(true);
                        case "autogg":
                            ;
                            break;
                        case "secure":
                            Toggles.toggleIgnoreCertificates(true);
                            break;
                        case "keepwho":
                            Toggles.toggleKeepWho(true);
                            break;
                        default:
                            ChatManager.send(Messages.USAGE);
                            return;
                    }
                    sendSettings();
                } else {
                    ChatManager.send(Messages.INVALID_COMMAND);
                }
                break;

            case "settings":
                sendSettings();
                break;

            case "help":
                ChatManager.send("§8[§cS§8] §7Available commands:");
                ChatManager.send("§c || §7/s api §b[API Key] §8: §7Sets Hypixel API Key to enable stats viewer.");
                ChatManager.send("§c || §7- You must get API Key from §ehttps://developer.hypixel.net");
                ChatManager.send("§c || §7/s flag §8: §7Sets Anticheat flag interval. It's up to you.");
                ChatManager.send("§c || §7/s bw §e[Player] -[Mode] §8: §7Shows their Bedwars stats in-game.");
                ChatManager.send("§c || §7/s sw §e[Player] -[Mode] §8: §7Shows their Skywars stats in-game.");
                ChatManager.send("§c || §7/s duels §e[Player] -[Mode] §8: §7Shows their Duels stats in-game.");
                ChatManager.send("§c || §7/s nh §e[Player] §8: §7Shows their Name History.");
                ChatManager.send("§c || §7/s autogg §8: §7Shows current AutoGG messages.");
                ChatManager.send("§c || §7/s autogg §e[Message] §8: §7Add new AutoGG message.");
                ChatManager.send("§c || §7- Keep it under 9 messages or get blocked for spamming.");
                ChatManager.send("§c || §7/s list §8: §7Toggles whether the stats list is displayed with /who.");
                ChatManager.send("§c || §7/s auto §8: §7Toggles auto stats viewer for Duels.");
                ChatManager.send("§c || §7/s denick §8: §7Toggles Denicker which can denick original skin users.");
                ChatManager.send("§c || §7- It's possibly bannable, use at your own risk.");
                ChatManager.send("§c || §7/s keepwho §8: §7Toggles whether the original /who message remains visible.");
                ChatManager.send("§c || §7/s skin §e[Player] §8: §7Download their skin locally.");
                ChatManager.send("§c || §7- Add -npcSkin to force saving NPC or Nick Skin if they have existing username.");
                ChatManager.send("§c || §7/s dir §e[Path] §8: §7Determines the directory to save skin files.");
                ChatManager.send("§c || §7/s add §e[Player] [Reason] §8: §7Reports cheaters to share and notify when you queued them.");
                ChatManager.send("§c || §7/s settings §8: §7Opens togglable settings");
                ChatManager.send("§c || §7/s secure §8: §7Toggles secure connections.");
                ChatManager.send("§c || §7- This should be disabled if you have errors while getting stats.");
                ChatManager.send("§c || §7- Usually, disabling this is not recommended as it can be insecure.");
                ChatManager.send(" ");
                ChatManager.send("§c || §7/s update §8: §7Check for latest version of the mod.");
                ChatManager.send("§c || §7/s help §8: §7Opens this help");
                ChatManager.send("§c || §7If you don't understand well, watch introduction video!");
                ChatManager.send("§c || §7 §ehttps://www.youtube.com/watch?v=(UPLOAD_SOON)");
                break;

            default:
                ChatManager.send(Messages.INVALID_COMMAND);
        }
    }

    private void handleFlagCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            ChatManager.send(Messages.USAGE);
            ChatManager.send("§c || §7Current value: §e§l" + Settings.getInstance().getFlagInterval() + "§7s");
            return;
        }

        try {
            double value = Double.parseDouble(args[1]);
            if (value < 0) value = 0;
            if (value > 20) value = 20;

            Settings.getInstance().setFlagInterval(value);

            ChatManager.send("§c || §7Set flag interval to §e§l" + value + "§7s");
        } catch (NumberFormatException e) {
            ChatManager.send("§c || §7Invalid value. Min: 0, Max: 20");
        }
    }

    private static final int SETTINGS_CHAT_ID = 99999;

    private static void sendSettings() {
        try {
            IChatComponent root = new ChatComponentText("§8[§cS§8] §7Settings:\n");

            String[][] settings = {
                    { "Denick", Toggles.denickEnabled ? "§b§lEnabled" : "§c§lDisabled", "denick",
                            "Toggle Denick §b§lEnabled / §c§lDisabled. \n§eDo not use denick if you want to be fully legit. This may cause of a Hypixel Ban." },
                    { "Bedwars Stats List", Toggles.listStatsEnabled ? "§b§lEnabled" : "§c§lDisabled", "listStats",
                            "Toggle Auto-Stats List with /who. \n§eWith this disabled, you can see original /who list." },
                    { "Auto Duels Stats", Toggles.autoStatsEnabled ? "§b§lEnabled" : "§c§lDisabled", "autoDuels",
                            "Toggle Auto Duels Stats. \n§eYou can get enemy stats automatically" },
                    { "Updated Duels Titles", Toggles.duelsUpdate ? "§b§lEnabled" : "§c§lDisabled", "duelsUpdate",
                            "Toggle New Duels Titles. \n§eWith this enabled, Duels Title can be shown with updated schemes." },
                    { "Secure Connection", !Toggles.ignoreCertificates ? "§b§lEnabled" : "§c§lDisabled", "secure",
                            "§c§lDo NOT Enable this! §eOnly use this to avoid fetching errors. \n§eThis lets you allow all certificates." },
                    { "Keep Original /who", Toggles.keepWhoEnabled ? "§b§lEnabled" : "§c§lDisabled", "keepwho",
                            "Keep original /who output visible while Bedwars Stats List Enabled." }
            };

            for (String[] s : settings) {
                IChatComponent line = new ChatComponentText("§c || §7" + s[0] + ": " + s[1] + "\n");

                line.getChatStyle().setChatClickEvent(
                        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/s toggle " + s[2]));

                line.getChatStyle().setChatHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("§e" + s[3])));

                root.appendSibling(line);
            }

            ChatOverlayManager.registerMessage(SETTINGS_CHAT_ID, root);

        } catch (Exception e) {
            e.printStackTrace();
            ChatManager.send("§8[§cS§8] §7Failed to open settings.");
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
