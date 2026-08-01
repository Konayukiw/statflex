package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class Title extends GuiComponentBase {
    private static final float SCALE = 1.45f;

    public Title(int id, int x, int y, int width, String text) {
        super(id, x, y, width, Math.round(Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT * SCALE) + 3, text);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.scale(SCALE, SCALE, 1.0f);
        fontRenderer.drawStringWithShadow(label, x / SCALE, y / SCALE,
                enabled ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_DISABLED);
        GlStateManager.popMatrix();
    }
}