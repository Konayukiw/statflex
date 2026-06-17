package com.konayuki.statflex.update;

import com.konayuki.statflex.update.UpdateChecker;
import com.konayuki.statflex.update.UpdateGuiHandler;
import com.konayuki.statflex.update.UpdateGuiScreen;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class UpdateGuiHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.currentScreen != null) return;
        if (!UpdateChecker.updateAvailable) return;
        if (UpdateChecker.guiShown) return;

        UpdateChecker.guiShown = true;
        mc.displayGuiScreen(new UpdateGuiScreen());
    }
}
