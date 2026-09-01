package com.konayuki.statflex.launch;

import com.konayuki.statflex.features.anticheat.Anticheat;
import com.konayuki.statflex.features.autogg.AutoGG;
import com.konayuki.statflex.features.bedwars.BedwarsList;
import com.konayuki.statflex.features.denick.Denick;
import com.konayuki.statflex.features.duels.DuelsList;
import com.konayuki.statflex.features.skywars.SkywarsList;
import com.konayuki.statflex.features.tab.TabStats;
import com.konayuki.statflex.features.rpc.DiscordRPC;
import com.konayuki.statflex.events.EventBus;
import com.konayuki.statflex.events.Subscribe;
import com.konayuki.statflex.events.TickEvent;
import com.konayuki.statflex.forge.ForgeBridge;
import com.konayuki.statflex.update.Update;
import com.konayuki.statflex.utils.*;
import com.konayuki.statflex.utils.Commands;
import com.konayuki.statflex.utils.api.HypixelApiUtil;
import com.konayuki.statflex.utils.chat.Chat;
import com.konayuki.statflex.utils.chat.Locraw;
import com.konayuki.statflex.utils.hypixel.Party;
import com.konayuki.statflex.utils.packet.PacketUtil;

import net.minecraft.client.Minecraft;

public final class Bootstrap {
    private static final Object LOCK = new Object();
    private static final TickHandler TICK_HANDLER = new TickHandler();
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

        System.setProperty("statflex.initialized", "true");

        try {
            System.setProperty("https.protocols", "TLSv1.2");

            Setting.load();
            Toggle.sync(Setting.get());
            Anticheat.register();
            PacketUtil.register();
            Commands.register();
            HypixelApiUtil.init();
            registerEvents(source);
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

    private static void registerEvents(String source) {
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

            EventBus.register(TICK_HANDLER);
            EventBus.register(locraw);
            EventBus.register(party);
            EventBus.register(bwList);
            EventBus.register(swList);
            EventBus.register(denick);
            EventBus.register(autoGG);
            EventBus.register(duelsList);
            EventBus.register(tabStats);

            if ("forge".equals(source)) {
                ForgeBridge.register();
            }

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

    private static final class TickHandler {
        @Subscribe
        public void onTick(TickEvent event) {
            PacketUtil.ensure();
            flushGreet();
            discordRPC.onTick();
        }
    }
}
