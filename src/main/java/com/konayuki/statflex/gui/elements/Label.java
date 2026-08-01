package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.Minecraft;

public class Label extends GuiComponentBase {

    private int color;

    public Label(int id, int x, int y, int width, String text) {
        super(id, x, y, width, Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT, text);
        this.color = GuiColors.TEXT_SECONDARY;
    }

    public Label(int id, int x, int y, int width, String text, int color) {
        super(id, x, y, width, Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT, text);
        this.color = color;
    }

    public void setText(String text) {
        this.label = text != null ? text : "";
    }

    public String getText() {
        return this.label;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void drawComponent(int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        fontRenderer.drawString(label, x, y, enabled ? color : GuiColors.TEXT_DISABLED);
    }
}