package com.konayuki.statflex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class UpdaterGui extends GuiScreen {

    @Override
    public void initGui() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.buttonList.add(new GuiButton(
                0,
                centerX - 110,
                centerY + 20,
                100,
                20,
                "Update"
        ));

        this.buttonList.add(new GuiButton(
                1,
                centerX + 10,
                centerY + 20,
                100,
                20,
                "Cancel"
        ));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.drawCenteredString(
                this.fontRendererObj,
                "New version: " + Updater.latestVersion + " is available.",
                centerX,
                centerY - 20,
                0xFFFFFF
        );

        this.drawCenteredString(
                this.fontRendererObj,
                "Click 'Update' for update. You will have to restart Minecraft.",
                centerX,
                centerY - 8,
                0xAAAAAA
        );

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            Updater.prepareUpdateAndExit();
        } else if (button.id == 1) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}