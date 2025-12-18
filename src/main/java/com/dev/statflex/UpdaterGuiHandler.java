package com.dev.statflex;

import com.dev.statflex.Updater;
import com.dev.statflex.UpdaterGui;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class UpdaterGuiHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.currentScreen != null) return;
        if (!Updater.updateAvailable) return;
        if (Updater.guiShown) return;

        Updater.guiShown = true;
        mc.displayGuiScreen(new UpdaterGui());
    }
}
