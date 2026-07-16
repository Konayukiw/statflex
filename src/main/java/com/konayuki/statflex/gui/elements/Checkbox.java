package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

public class Checkbox extends GuiComponentBase {

    public interface OnValueChanged {
        void accept(boolean value);
    }

    private boolean isChecked;
    private final float cornerRadius = 2f;
    private final float checkmarkInsetRatio = 0.25f;
    private final OnValueChanged onValueChanged;

    public Checkbox(int id, int x, int y, int boxSize, String label, boolean initialValue, OnValueChanged onValueChanged) {
        super(id, x, y, boxSize, boxSize, label);
        this.isChecked = initialValue;
        this.onValueChanged = onValueChanged;
    }

    public Checkbox(int id, int x, int y, String label, boolean initialValue, OnValueChanged onValueChanged) {
        this(id, x, y, MODERN_CHECKBOX_SIZE, label, initialValue, onValueChanged);
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked, boolean notify) {
        if (this.isChecked != checked) {
            this.isChecked = checked;
            if (notify && onValueChanged != null) {
                onValueChanged.accept(isChecked);
            }
        } else {
            this.isChecked = checked;
        }
    }

    @Override
    public void drawComponent(int mouseX, int mouseY, float partialTicks) {
        super.drawComponent(mouseX, mouseY, partialTicks);
        if (!visible) {
            return;
        }

        boolean boxActuallyHovered = this.hovered || (isLabelHovered(mouseX, mouseY) && enabled);

        int currentBgColor;
        int currentBorderColor;
        int currentCheckColor;

        if (!enabled) {
            currentBgColor = GuiColors.COMPONENT_BACKGROUND_DISABLED;
            currentBorderColor = GuiColors.MODERN_UI_ELEMENT_BORDER;
            currentCheckColor = GuiColors.TEXT_DISABLED;
        } else {
            currentBgColor = boxActuallyHovered ? GuiColors.CHECKBOX_BOX_HOVER : GuiColors.CHECKBOX_BOX;
            currentBorderColor = (boxActuallyHovered || isChecked)
                    ? GuiColors.PRIMARY_BLUE_BRIGHT
                    : GuiColors.MODERN_UI_ELEMENT_BORDER;
            currentCheckColor = GuiColors.CHECKBOX_CHECK;
        }

        drawRoundedRectUsingGL(
                this.x + SHADOW_OFFSET_X / 2f,
                this.y + SHADOW_OFFSET_Y / 2f,
                width, height,
                cornerRadius,
                GuiColors.SUBTLE_SHADOW_COLOR
        );

        float borderThickness = MODERN_BORDER_THICKNESS;

        drawRoundedRectUsingGL(this.x, this.y, width, height, cornerRadius, currentBorderColor);
        drawRoundedRectUsingGL(
                this.x + borderThickness, this.y + borderThickness,
                width - borderThickness * 2, height - borderThickness * 2,
                Math.max(0f, cornerRadius - borderThickness),
                currentBgColor
        );

        if (isChecked) {
            int inset = Math.max(1, (int) (width * checkmarkInsetRatio));
            drawRoundedRectUsingGL(
                    this.x + inset, this.y + inset,
                    width - 2 * inset, height - 2 * inset,
                    1f,
                    currentCheckColor
            );
        }

        drawSideLabel((height - fontRenderer.FONT_HEIGHT) / 2 + 1, 6);
    }

    private boolean isLabelHovered(int mouseX, int mouseY) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        int labelStartX = x + width + 6;
        int labelRenderY = y + (height - fontRenderer.FONT_HEIGHT) / 2 + 1;
        int labelTextWidth = fontRenderer.getStringWidth(label);
        return mouseX >= labelStartX && mouseX < labelStartX + labelTextWidth
                && mouseY >= labelRenderY && mouseY < labelRenderY + fontRenderer.FONT_HEIGHT;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (enabled && visible && mouseButton == 0) {
            boolean boxClicked = mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
            boolean labelClicked = isLabelHovered(mouseX, mouseY);

            if (boxClicked || labelClicked) {
                isChecked = !isChecked;
                if (onValueChanged != null) {
                    onValueChanged.accept(isChecked);
                }
                mc.getSoundHandler().playSound(
                        PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 0.7F));
                return true;
            }
        }
        return false;
    }
}
