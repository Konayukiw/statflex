package com.konayuki.statflex;

import com.konayuki.statflex.anticheat.Anticheat;
import com.konayuki.statflex.anticheat.RuntimePacketHook;
import com.konayuki.statflex.command.Commands;
import com.konayuki.statflex.config.ApiKeyManager;
import com.konayuki.statflex.config.Toggles;
import com.konayuki.statflex.config.Settings;
import com.konayuki.statflex.feature.denick.Denicker;
import com.konayuki.statflex.system.Messages;
import com.konayuki.statflex.feature.autogg.AutoGG;
import com.konayuki.statflex.stats.bedwars.BedwarsStatsLister;
import com.konayuki.statflex.stats.duels.ScoreboardManager;
import com.konayuki.statflex.update.UpdateChecker;
import com.konayuki.statflex.update.UpdateGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class JVMBoostrap {
    private static final Object LOCK = new Object();
    private static final LifecycleHandler LIFECYCLE_HANDLER = new LifecycleHandler();

    private static boolean initialized;
    private static boolean eventHandlersRegistered;
    private static boolean updateCheckStarted;
    private static boolean pendingLoadedMessage;
    private static String initializedBy = "unknown";

    private static BedwarsStatsLister bwListStats;
    private static ScoreboardManager duelsListStats;
    private static Denicker denicker;
    private static AutoGG autoGG;
    private static UpdateGuiHandler updaterGuiHandler;

    private JVMBoostrap() {
    }

    public static boolean startFromForge() {
        return start("forge");
    }

    public static boolean startFromInjection() {
        return start("injection");
    }

    public static boolean start(String source) {
        System.out.println("[S] Debug: start() success");
        synchronized (LOCK) {
            if (initialized) {
                System.out.println("[S] statflex is already initialized by " + initializedBy + "; ignored duplicate request from " + source + ".");
                requestLoadedMessage();
                return false;
            }

            initialized = true;
            initializedBy = source;
        }

        try {
            System.setProperty("https.protocols", "TLSv1.2");

            System.out.println("[S] Debug: try{} reached");
            Settings.load();
            System.out.println("[S] Debug: Settings loaded");
            Toggles.syncFromSettings(Settings.getInstance());
            System.out.println("[S] Debug: getInstance() success");
            ApiKeyManager.init();
            System.out.println("[S] Debug: API Manager initialized");
            registerEventHandlers();
            System.out.println("[S] Debug: Event handlers registered");
            Commands.register();
            System.out.println("[S] Debug: Commands registered");
            Anticheat.register();
            System.out.println("[S] Debug: Anticheat registered");
            RuntimePacketHook.register();
            System.out.println("[S] Debug: RuntimePacketHook registered");
            startUpdateCheck();
            System.out.println("[S] Debug: UpdateCheck ran");

            requestLoadedMessage();
            System.out.println("[S] Debug: initialization completed: " + source + ".");
            return true;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    private static void registerEventHandlers() {
        synchronized (LOCK) {
            if (eventHandlersRegistered) {
                return;
            }

            bwListStats = new BedwarsStatsLister();
            duelsListStats = new ScoreboardManager();
            denicker = new Denicker();
            autoGG = new AutoGG();
            updaterGuiHandler = new UpdateGuiHandler();

            MinecraftForge.EVENT_BUS.register(LIFECYCLE_HANDLER);
            MinecraftForge.EVENT_BUS.register(bwListStats);
            MinecraftForge.EVENT_BUS.register(duelsListStats);
            MinecraftForge.EVENT_BUS.register(denicker);
            MinecraftForge.EVENT_BUS.register(autoGG);
            MinecraftForge.EVENT_BUS.register(updaterGuiHandler);
            eventHandlersRegistered = true;
        }
    }

    private static void startUpdateCheck() {
        synchronized (LOCK) {
            if (updateCheckStarted) {
                return;
            }
            updateCheckStarted = true;
        }

        UpdateChecker.checkForUpdatesAsync();
    }

    private static void requestLoadedMessage() {
        pendingLoadedMessage = true;
        runOnClientThread(new Runnable() {
            @Override
            public void run() {
                flushLoadedMessage();
            }
        });
    }

    private static void flushLoadedMessage() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!pendingLoadedMessage || mc == null || mc.thePlayer == null) {
            return;
        }

        pendingLoadedMessage = false;
        mc.thePlayer.addChatMessage(new ChatComponentText(Messages.PREFIX + "statflex has been loaded!"));
    }

    private static void runOnClientThread(Runnable runnable) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
                mc.addScheduledTask(runnable);
                return;
            }
        } catch (Throwable ignored) {
        }

        runnable.run();
    }

    private static final class LifecycleHandler {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            RuntimePacketHook.ensureInstalled();
            flushLoadedMessage();
        }
    }
}
