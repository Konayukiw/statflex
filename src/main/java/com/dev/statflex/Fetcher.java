package com.dev.statflex;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Fetcher implements ICommand {

    private final List<String> aliases = Arrays.asList("s");

    private static final AutoGG AutoGGInstance = new AutoGG();

    public static boolean listStatsEnabled;
    public static boolean autoStatsEnabled;
    public static boolean ignoreCertificates;
    public static boolean denickenabled;
    public static boolean duelsUpdate;
    public static boolean keepWhoEnabled;

    public static boolean isListStatsEnabled() {
        return listStatsEnabled;
    }

    public static boolean isAutoStatsEnabled() {
        return autoStatsEnabled;
    }

    public static boolean isIgnoreCertificatesEnabled() {
        return ignoreCertificates;
    }

    public static boolean isKeepWhoEnabled() {
        return keepWhoEnabled;
    }

    public static void syncFromSettings(SettingsManager settings) {
        listStatsEnabled = settings.listStatsEnabled;
        autoStatsEnabled = settings.autoStatsEnabled;
        ignoreCertificates = settings.ignoreCertificates;
        denickenabled = settings.denickEnabled;
        duelsUpdate = settings.duelsUpdate;
        keepWhoEnabled = settings.keepWhoEnabled;

    }

    public static void toggleListStats(boolean silent) {
        listStatsEnabled = !listStatsEnabled;
        SettingsManager.getInstance().listStatsEnabled = listStatsEnabled;
        SettingsManager.save();
        if (!silent) {
            sendChat(listStatsEnabled
                    ? "§8[§cS§8]§7 Stats list has been §b§lEnabled"
                    : "§8[§cS§8]§7 Stats list has been §c§lDisabled");
        }
    }

    public static void toggleAutoStats(boolean silent) {
        autoStatsEnabled = !autoStatsEnabled;
        SettingsManager.getInstance().autoStatsEnabled = autoStatsEnabled;
        SettingsManager.save();
        if (!silent) {
            sendChat(autoStatsEnabled
                    ? "§8[§cS§8]§7 Duels Auto-Stats has been §b§lEnabled"
                    : "§8[§cS§8]§7 Duels Auto-Stats has been §c§lDisabled");
        }
    }

    public static void toggleDenick(boolean silent) {
        denickenabled = !denickenabled;
        SettingsManager.getInstance().denickEnabled = denickenabled;
        SettingsManager.save();
        if (!silent) {
            String msg = denickenabled
                    ? "§8[§cS§8]§7 Denick detection has been §b§lEnabled"
                    : "§8[§cS§8]§7 Denick detection has been §c§lDisabled";
            sendChat(msg);
        }
    }

    public static void toggleDuelsUpdate(boolean silent) {
        duelsUpdate = !duelsUpdate;
        SettingsManager.getInstance().duelsUpdate = duelsUpdate;
        SettingsManager.save();
        if (!silent) {
            String msg = duelsUpdate
                    ? "§8[§cS§8]§7 Updated Duels Titles has been §b§lEnabled"
                    : "§8[§cS§8]§7 Updated Duels Titles has been §c§lDisabled";
            sendChat(msg);
        }
    }

    public static void toggleIgnoreCertificates(boolean silent) {
        ignoreCertificates = !ignoreCertificates;
        SettingsManager.getInstance().ignoreCertificates = ignoreCertificates;
        SettingsManager.save();
        if (!silent) {
            sendChat(ignoreCertificates
                    ? "§8[§cS§8]§7 Secure connection has been §c§lDisabled"
                    : "§8[§cS§8]§7 Secure connection has been §b§lEnabled");
        }
    }

    public static void toggleKeepWho(boolean silent) {
        keepWhoEnabled = !keepWhoEnabled;
        SettingsManager.getInstance().keepWhoEnabled = keepWhoEnabled;
        SettingsManager.save();
        if (!silent) {
            String msg = keepWhoEnabled
                    ? "§8[§cS§8]§7 Original /who keeper has been §b§lEnabled"
                    : "§8[§cS§8]§7 Original /who keeper has been §c§lDisabled";
            sendChat(msg);
        }
    }

    public static void setIgnoreCertificates(boolean ignore) {
        ignoreCertificates = ignore;
    }

    public static boolean isDuelsUpdateEnabled() {
        return duelsUpdate;
    }

    public static void register() {
        ClientCommandHandler.instance.registerCommand(new Fetcher());
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
            sendChat(Messages.USAGE);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "api":
                if (args.length < 2) {
                    sendChat(Messages.USAGE);
                    return;
                }
                String key = args[1];

                ApiKeyManager.setApiKey(args[1]);
                ApiKeyManager.setApiKey(key);
                SettingsManager.getInstance().apiKey = key;
                SettingsManager.save();
                sendChat(Messages.API_SET);
                break;

            case "bw":
                if (args.length < 2) {
                    sendChat(Messages.USAGE);
                    return;
                }
                String bwName = args[1];
                String bwMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                BwFetcher.fetchStats(bwName, bwMode);
                break;

            case "sw":
                if (args.length < 2) {
                    sendChat(Messages.USAGE);
                    return;
                }
                String swName = args[1];
                String swMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                SwFetcher.fetchStats(swName, swMode);
                break;

            case "duels":
                if (args.length < 2) {
                    sendChat(Messages.USAGE);
                    return;
                }
                String duelsName = args[1];
                String duelsMode = (args.length >= 3 && args[2].startsWith("-")) ? args[2].substring(1) : null;
                if (duelsUpdate) {
                    DuelsFetcherForUpdate.fetchStats(duelsName, duelsMode);
                } else {
                    DuelsFetcher.fetchStats(duelsName, duelsMode);
                }
                break;

            case "duelsupdate":
                toggleDuelsUpdate(false);
                break;

            case "list":
                toggleListStats(false);
                break;

            case "auto":
                toggleAutoStats(false);
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

            case "autogg":
                if (args.length == 1) {
                    AutoGGInstance.showMessages();
                } else {
                    String[] messageArgs = Arrays.copyOfRange(args, 1, args.length);
                    AutoGGInstance.handleCommand(messageArgs);
                }
                break;

            case "secure":
                toggleIgnoreCertificates(false);
                break;

            case "denick":
                toggleDenick(false);
                break;

            case "keepwho":
                toggleKeepWho(false);
                break;

            case "toggle":
                if (args.length >= 2) {
                    String setting = args[1].toLowerCase();
                    switch (setting) {
                        case "liststats":
                            Fetcher.toggleListStats(true);
                            break;
                        case "autoduels":
                            Fetcher.toggleAutoStats(true);
                            break;
                        case "duelsupdate":
                            toggleDuelsUpdate(true);
                            break;
                        case "denick":
                            toggleDenick(true);
                            // currently developing
                        case "autogg":
                            ;
                            break;
                        case "secure":
                            toggleIgnoreCertificates(true);
                            break;
                        case "keepwho":
                            toggleKeepWho(true);
                            break;
                        default:
                            sendChat(Messages.INVALID_COMMAND);
                            return;
                    }
                    sendSettings();
                } else {
                    sendChat(Messages.INVALID_COMMAND);
                }
                break;

            case "settings":
                sendSettings();
                break;

            case "help":
                sendChat("§8[§cS§8] §7Available commands:");
                sendChat("§c || §7/s api §b[API Key] §8: §7Sets Hypixel API Key to enable stats viewer.");
                sendChat("§c || §7- §7You must get API Key from §ehttps://developer.hypixel.net");
                sendChat("§c || §7/s bw §e[Player] -[Mode] §8: §7Shows their Bedwars stats in-game.");
                sendChat("§c || §7/s sw §e[Player] -[Mode] §8: §7Shows their Skywars stats in-game.");
                sendChat("§c || §7/s duels §e[Player] -[Mode] §8: §7Shows their Duels stats in-game.");
                sendChat("§c || §7/s nh §e[Player] §8: §7Shows their Name History.");
                sendChat("§c || §7/s autogg §8: §7Shows current AutoGG messages.");
                sendChat("§c || §7/s autogg §e[Message] §8: §7Add new AutoGG message.");
                sendChat("§c || §7- §7Keep it under 9 messages or get blocked for spamming.");
                sendChat("§c || §7/s list §8: §7Toggles whether the stats list is displayed with /who.");
                sendChat("§c || §7/s auto §8: §7Toggles auto stats viewer for Duels.");
                sendChat("§c || §7/s denick §8: §7Toggles Denicker which can denick original skin users.");
                sendChat("§c || §7- §7It's possibly bannable, use at your own risk.");
                sendChat("§c || §7/s keepwho §8: §7Toggles whether the original /who message remains visible.");
                sendChat("§c || §7/s settings §8: §7Opens togglable settings");
                sendChat("§c || §7/s secure §8: §7Toggles secure connections.");
                sendChat("§c || §7- §7This should be disabled if you have errors while getting stats.");
                sendChat("§c || §7- §7 Usually, disabling this is not recommended as it can be insecure.");
                sendChat(" ");
                sendChat("§c || §7/s help §8: §7Opens this help");
                sendChat("§c || §7If you don't understand well, watch introduction video!");
                sendChat("§c || §7 §ehttps://www.youtube.com/watch?v=(UPLOAD_SOON)");
                break;

            default:
                sendChat(Messages.INVALID_COMMAND);
        }
    }

    private static final int SETTINGS_CHAT_ID = 99999;

    private static void sendSettings() {
        try {
            IChatComponent root = new ChatComponentText("§8[§cS§8] §7Settings:\n");

            String[][] settings = {
                    { "Denick", denickenabled ? "§b§lEnabled" : "§c§lDisabled", "denick",
                            "Toggle Denicker §b§lEnabled / §c§lDisabled. \n§eDo not use denicker if you want to be fully legit. This may cause of a Hypixel Ban." },
                    { "Bedwars Stats List", listStatsEnabled ? "§b§lEnabled" : "§c§lDisabled", "listStats",
                            "Toggle Auto-Stats List with /who. \n§eWith this disabled, you can see original /who list." },
                    { "Auto Duels Stats", autoStatsEnabled ? "§b§lEnabled" : "§c§lDisabled", "autoDuels",
                            "Toggle Auto Duels Stats. \n§eYou can get enemy stats automatically" },
                    { "Updated Duels Titles", duelsUpdate ? "§b§lEnabled" : "§c§lDisabled", "duelsUpdate",
                            "Toggle New Duels Titles. \n§eWith this enabled, Duels Title can be shown with updated schemes." },
                    { "Secure Connection", !ignoreCertificates ? "§b§lEnabled" : "§c§lDisabled", "secure",
                            "§c§lDo NOT Enable this! §eOnly use this to avoid fetching errors. \n§eThis lets you allow all certificates." },
                    { "Keep Original /who", keepWhoEnabled ? "§b§lEnabled" : "§c§lDisabled", "keepwho",
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

            SettingsMessageManager.registerMessage(SETTINGS_CHAT_ID, root);

        } catch (Exception e) {
            e.printStackTrace();
            sendChat("[S] Failed to open settings.");
        }
    }

    /*
     * private static void checkApiKeyAsync(IChatComponent root) {
     * new Thread(() -> {
     * String apiKey = ApiKeyManager.getApiKey();
     * String msg;
     * 
     * if (apiKey == null || apiKey.isEmpty()) {
     * msg =
     * "§c || §7No API Key set. Generate key from §chttps://developer.hypixel.net";
     * } else {
     * try {
     * java.net.URL url = new java.net.URL("https://api.hypixel.net/key?key=" +
     * apiKey);
     * java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
     * url.openConnection();
     * conn.setRequestMethod("GET");
     * conn.setConnectTimeout(5000);
     * conn.setReadTimeout(5000);
     * 
     * java.io.InputStreamReader reader = new
     * java.io.InputStreamReader(conn.getInputStream());
     * com.google.gson.JsonObject response = new
     * com.google.gson.JsonParser().parse(reader)
     * .getAsJsonObject();
     * 
     * boolean success = response.get("success").getAsBoolean();
     * if (success) {
     * msg = "§c || §7API Key is valid.";
     * } else {
     * msg =
     * "§c || §7API Key is invalid! Get API key from §chttps://developer.hypixel.net"
     * ;
     * }
     * } catch (Exception e) {
     * msg = "§c || §7Could not verify API Key: Network Error";
     * }
     * }
     * 
     * final String finalMsg = msg;
     * Minecraft.getMinecraft().addScheduledTask(() -> {
     * root.appendSibling(new ChatComponentText(finalMsg));
     * SettingsMessageManager.registerMessage(SETTINGS_CHAT_ID, root);
     * });
     * }).start();
     * }
     * 
     */

    /*
     * private static IChatComponent makeToggle(String name, boolean enabled, String
     * cmd, String description) {
     * String state = enabled ? "§b§lEnabled" : "§c§lDisabled";
     * ChatComponentText text = new ChatComponentText("§7" + name + ": " + state);
     * 
     * text.getChatStyle().setChatClickEvent(
     * new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
     * 
     * text.getChatStyle().setChatHoverEvent(
     * new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText("§e" +
     * description)));
     * 
     * return text;
     * }
     */

    private static void sendChat(String msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(msg));
            }
        });
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
