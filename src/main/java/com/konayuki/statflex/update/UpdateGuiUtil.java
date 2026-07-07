package com.konayuki.statflex.update;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class UpdateGuiUtil {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.currentScreen != null) return;
        if (!Update.updateAvailable) return;
        if (Update.guiShown) return;

        Update.guiShown = true;
        mc.displayGuiScreen(new UpdateGui());
    }
}
