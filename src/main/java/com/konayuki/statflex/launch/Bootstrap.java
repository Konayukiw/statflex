package com.konayuki.statflex.launch;

import com.konayuki.statflex.features.anticheat.Anticheat;
import com.konayuki.statflex.update.Update;
import com.konayuki.statflex.utils.*;
import com.konayuki.statflex.utils.Commands;
import com.konayuki.statflex.features.denick.Denick;
import com.konayuki.statflex.features.autogg.AutoGG;
import com.konayuki.statflex.features.bedwars.BedwarsList;
import com.konayuki.statflex.features.duels.DuelsList;
import com.konayuki.statflex.features.skywars.SkywarsList;
import com.konayuki.statflex.features.tab.TabStats;
import com.konayuki.statflex.features.rpc.DiscordRPC;

import com.konayuki.statflex.utils.api.HypixelApiUtil;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.utils.hypixel.Party;
import com.konayuki.statflex.utils.packet.PacketUtil;
import net.minecraft.client.Minecraft;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class Bootstrap {
    private static final Object LOCK = new Object();
    private static final LifecycleHandler LIFECYCLE_HANDLER = new LifecycleHandler();
    private static boolean initialized;
    private static boolean eventHandlersRegistered;
    private static boolean updateCheckStarted;
    private static boolean pendingLoadedMessage;
    private static String initializedBy = "unknown";
    private static BedwarsList bwList;
    private static SkywarsList swList;
    private static Denick denick;
    private static AutoGG autoGG;
    private static DuelsList duelsList;
    private static TabStats tabStats;
    private static Locraw locraw;
    private static Party party;
    private static DiscordRPC discordRPC;

    private Bootstrap() {
    }

    public static boolean start(String source) {
        synchronized (LOCK) {
            if (initialized) {
                Debug.log("statflex is already initialized by " + initializedBy + "; ignored duplicate request from " + source + ".");
                greet();
                return false;
            }

            initialized = true;
            initializedBy = source;
        }

        try {
            System.setProperty("https.protocols", "TLSv1.2");

            Setting.load();
            Toggle.sync(Setting.get());
            Anticheat.register();
            PacketUtil.register();
            Commands.register();
            HypixelApiUtil.init();
            registerEvents();
            checkUpdate();
            greet();
            return true;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    public static boolean fromForge() {
        return start("forge");
    }

    public static boolean fromInjection() {
        return start("injection");
    }

    private static void registerEvents() {
        synchronized (LOCK) {
            if (eventHandlersRegistered) {
                return;
            }

            bwList = new BedwarsList();
            swList = new SkywarsList();
            denick = new Denick();
            autoGG = new AutoGG();
            duelsList = new DuelsList();
            tabStats = new TabStats();
            locraw = Locraw.get();
            party = Party.get();
            discordRPC = DiscordRPC.get();

            MinecraftForge.EVENT_BUS.register(LIFECYCLE_HANDLER);
            MinecraftForge.EVENT_BUS.register(locraw);
            MinecraftForge.EVENT_BUS.register(party);
            MinecraftForge.EVENT_BUS.register(bwList);
            MinecraftForge.EVENT_BUS.register(swList);
            MinecraftForge.EVENT_BUS.register(denick);
            MinecraftForge.EVENT_BUS.register(autoGG);
            MinecraftForge.EVENT_BUS.register(duelsList);
            MinecraftForge.EVENT_BUS.register(tabStats);
            eventHandlersRegistered = true;
        }
    }

    private static void checkUpdate() {
        synchronized (LOCK) {
            if (updateCheckStarted) {
                return;
            }
            updateCheckStarted = true;
        }

        Update.checkAsync();
    }

    private static void greet() {
        pendingLoadedMessage = true;
        onClient(new Runnable() {
            @Override
            public void run() {
                flushGreet();
            }
        });
    }

    private static void flushGreet() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!pendingLoadedMessage || mc == null || mc.thePlayer == null) {
            return;
        }

        pendingLoadedMessage = false;
        Chat.send(Messages.PREFIX + "statflex has been loaded!");
    }

    private static void onClient(Runnable runnable) {
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
        public void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            PacketUtil.ensure();
            flushGreet();
            DiscordRPC.get().onTick();
        }
    }
}