package com.konayuki.statflex.forge;

import com.konayuki.statflex.events.EventBus;
import com.konayuki.statflex.events.MouseEvent;
import com.konayuki.statflex.events.RenderTabEvent;
import com.konayuki.statflex.events.TickEvent;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ForgeBridge {

    private ForgeBridge() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ForgeBridge());
        ClientCommandHandler.instance.registerCommand(new ForgeCommands());
    }

    @SubscribeEvent
    public void onTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
            EventBus.post(TickEvent.get());
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            return;
        }
        RenderTabEvent tab = new RenderTabEvent(event.partialTicks);
        EventBus.post(tab);
        if (tab.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouse(net.minecraftforge.client.event.MouseEvent event) {
        if (event.dwheel == 0) {
            return;
        }
        MouseEvent wheel = new MouseEvent(event.dwheel);
        EventBus.post(wheel);
        if (wheel.isCancelled()) {
            event.setCanceled(true);
        }
    }
}
