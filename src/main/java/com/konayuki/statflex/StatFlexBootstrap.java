package com.konayuki.statflex;

import com.konayuki.statflex.anticheat.Anticheat;
import com.konayuki.statflex.anticheat.RuntimePacketHook;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class StatFlexBootstrap {
    private static final Object LOCK = new Object();
    private static final LifecycleHandler LIFECYCLE_HANDLER = new LifecycleHandler();

    private static boolean initialized;
    private static boolean eventHandlersRegistered;
    private static boolean updateCheckStarted;
    private static boolean pendingLoadedMessage;
    private static String initializedBy = "unknown";

    private static BwListStats bwListStats;
    private static DuelsListStats duelsListStats;
    private static Denicker denicker;
    private static AutoGG autoGG;
    private static UpdaterGuiHandler updaterGuiHandler;

    private StatFlexBootstrap() {
    }

    public static boolean startFromForge() {
        return start("forge");
    }

    public static boolean startFromInjection() {
        return start("injection");
    }

    public static boolean start(String source) {
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

            SettingsManager.load();
            Fetcher.syncFromSettings(SettingsManager.getInstance());
            ApiKeyManager.init();

            registerEventHandlers();
            Fetcher.register();
            Anticheat.register();
            RuntimePacketHook.register();
            startUpdateCheck();

            requestLoadedMessage();
            System.out.println("[S] statflex initialized from " + source + ".");
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

            bwListStats = new BwListStats();
            duelsListStats = new DuelsListStats();
            denicker = new Denicker();
            autoGG = new AutoGG();
            updaterGuiHandler = new UpdaterGuiHandler();

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

        Updater.checkForUpdatesAsync();
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
