package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

public class Button extends GuiComponentBase {
    private final float cornerRadius = MODERN_CORNER_RADIUS;
    private final OnClick onClick;

    public Button(int id, int x, int y, int width, int height, String buttonText, OnClick onClick) {
        super(id, x, y, width, height, buttonText);
        this.onClick = onClick;
    }

    public Button(int id, int x, int y, int width, String buttonText, OnClick onClick) {
        this(id, x, y, width, MODERN_BUTTON_HEIGHT, buttonText, onClick);
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        super.draw(mouseX, mouseY, partialTicks);
        if (!visible) {
            return;
        }

        int currentBgColor;
        int currentTextColor;
        int currentBorderColor;
        int glowColor = 0;

        if (!enabled) {
            currentBgColor = GuiColors.COMPONENT_BACKGROUND_DISABLED;
            currentTextColor = GuiColors.TEXT_DISABLED;
            currentBorderColor = GuiColors.MODERN_UI_ELEMENT_BORDER;
        } else if (hovered) {
            currentBgColor = GuiColors.BUTTON_MODERN_BACKGROUND_HOVER;
            currentTextColor = GuiColors.BUTTON_MODERN_TEXT;
            currentBorderColor = GuiColors.PRIMARY_BLUE_DARK;
            glowColor = GuiColors.PRIMARY_BLUE_BRIGHT_GLOW_EFFECT;
        } else {
            currentBgColor = GuiColors.BUTTON_MODERN_BACKGROUND;
            currentTextColor = GuiColors.BUTTON_MODERN_TEXT;
            currentBorderColor = GuiColors.PRIMARY_BLUE_DARK;
        }

        float borderThickness = MODERN_BORDER_THICKNESS;

        if (glowColor != 0) {
            roundRect(
                    x - 1f, y - 1f,
                    width + 2f, height + 2f,
                    cornerRadius + 1f,
                    glowColor
            );
        }

        roundRect(x, y, width, height, cornerRadius, currentBorderColor);
        roundRect(
                x + borderThickness, y + borderThickness,
                width - borderThickness * 2, height - borderThickness * 2,
                Math.max(0f, cornerRadius - borderThickness),
                currentBgColor
        );

        int textY = y + (height - fontRenderer.FONT_HEIGHT) / 2 + 1;
        centered(fontRenderer, label, x + width / 2, textY, currentTextColor);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int mouseButton) {
        if (enabled && visible && mouseButton == 0
                && mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height) {
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
            if (onClick != null) {
                onClick.run();
            }
            return true;
        }
        return false;
    }

    private void centered(FontRenderer fr, String text, int x, int y, int color) {
        fr.drawString(text, x - fr.getStringWidth(text) / 2, y, color);
    }

    public interface OnClick {
        void run();
    }
}