package com.konayuki.statflex.launch;

import com.konayuki.statflex.features.anticheat.Anticheat;
import com.konayuki.statflex.update.Update;
import com.konayuki.statflex.update.UpdateGuiUtil;
import com.konayuki.statflex.utils.*;
import com.konayuki.statflex.utils.Command;
import com.konayuki.statflex.features.denick.Denick;
import com.konayuki.statflex.features.autogg.AutoGG;
import com.konayuki.statflex.features.bedwars.BedwarsList;
import com.konayuki.statflex.features.duels.DuelsList;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class JVMBootstrap {
    private static final Object LOCK = new Object();
    private static final LifecycleHandler LIFECYCLE_HANDLER = new LifecycleHandler();

    private static boolean initialized;
    private static boolean eventHandlersRegistered;
    private static boolean updateCheckStarted;
    private static boolean pendingLoadedMessage;
    private static String initializedBy = "unknown";

    private static BedwarsList bwList;
    private static Denick denick;
    private static AutoGG autoGG;
    private static UpdateGuiUtil updaterGuiHandler;
    private static DuelsList duelsList;
    private static Locraw locraw;

    private JVMBootstrap() {
    }

    public static boolean startFromForge() {
        return start("forge");
    }

    public static boolean startFromInjection() {
        return start("injection");
    }

    public static boolean start(String source) {
        Debug.log("Debug: start() success");
        synchronized (LOCK) {
            if (initialized) {
                Debug.log("statflex is already initialized by " + initializedBy + "; ignored duplicate request from " + source + ".");
                requestLoadedMessage();
                return false;
            }

            initialized = true;
            initializedBy = source;
        }

        try {
            System.setProperty("https.protocols", "TLSv1.2");

            Settings.load();
            Toggles.syncFromSettings(Settings.getInstance());
            Anticheat.register();
            PacketUtil.register();
            HypixelApiUtil.init();
            registerEventHandlers();
            Command.register();
            startUpdateCheck();
            requestLoadedMessage();
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

            bwList = new BedwarsList();
            denick = new Denick();
            autoGG = new AutoGG();
            updaterGuiHandler = new UpdateGuiUtil();
            duelsList = new DuelsList();
            locraw = Locraw.getInstance();

            MinecraftForge.EVENT_BUS.register(LIFECYCLE_HANDLER);
            MinecraftForge.EVENT_BUS.register(locraw);
            MinecraftForge.EVENT_BUS.register(bwList);
            MinecraftForge.EVENT_BUS.register(denick);
            MinecraftForge.EVENT_BUS.register(autoGG);
            MinecraftForge.EVENT_BUS.register(updaterGuiHandler);
            MinecraftForge.EVENT_BUS.register(duelsList);
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

        Update.checkForUpdatesAsync();
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

            PacketUtil.ensureInstalled();
            flushLoadedMessage();
        }
    }
}
