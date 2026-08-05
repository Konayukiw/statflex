package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class Checkbox extends GuiComponentBase {
    private static final float DESCRIPTION_SCALE = 0.75f;
    private static final int DESCRIPTION_GAP = 2;
    private static final int LABEL_X_OFFSET = 6;
    private boolean isChecked;
    private final float cornerRadius = 2f;
    private final float checkmarkInsetRatio = 0.25f;
    private final OnValueChanged onValueChanged;
    private final String description;
    private final int boxSize;

    public Checkbox(int id, int x, int y, int boxSize, String label, String description,
                    boolean initialValue, OnValueChanged onValueChanged) {
        super(id, x, y, boxSize, boxSize, label);
        this.boxSize = boxSize;
        this.isChecked = initialValue;
        this.onValueChanged = onValueChanged;
        this.description = description != null ? description : "";
        resize();
    }

    public Checkbox(int id, int x, int y, int boxSize, String label, boolean initialValue, OnValueChanged onValueChanged) {
        this(id, x, y, boxSize, label, null, initialValue, onValueChanged);
    }

    public Checkbox(int id, int x, int y, String label, boolean initialValue, OnValueChanged onValueChanged) {
        this(id, x, y, MODERN_CHECKBOX_SIZE, label, null, initialValue, onValueChanged);
    }

    public Checkbox(int id, int x, int y, String label, String description,
                    boolean initialValue, OnValueChanged onValueChanged) {
        this(id, x, y, MODERN_CHECKBOX_SIZE, label, description, initialValue, onValueChanged);
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        super.draw(mouseX, mouseY, partialTicks);
        if (!visible) {
            return;
        }

        boolean boxActuallyHovered = this.hovered || (onLabel(mouseX, mouseY) && enabled);

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

        roundRect(
                this.x + SHADOW_OFFSET_X / 2f,
                this.y + SHADOW_OFFSET_Y / 2f,
                boxSize, boxSize,
                cornerRadius,
                GuiColors.SUBTLE_SHADOW_COLOR
        );

        float borderThickness = MODERN_BORDER_THICKNESS;

        roundRect(this.x, this.y, boxSize, boxSize, cornerRadius, currentBorderColor);
        roundRect(
                this.x + borderThickness, this.y + borderThickness,
                boxSize - borderThickness * 2, boxSize - borderThickness * 2,
                Math.max(0f, cornerRadius - borderThickness),
                currentBgColor
        );

        if (isChecked) {
            int inset = Math.max(1, (int) (boxSize * checkmarkInsetRatio));
            roundRect(
                    this.x + inset, this.y + inset,
                    boxSize - 2 * inset, boxSize - 2 * inset,
                    1f,
                    currentCheckColor
            );
        }

        int labelY = y + (boxSize - fontRenderer.FONT_HEIGHT) / 2 + 1;
        int labelColor = enabled ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_DISABLED;
        if (label != null && !label.isEmpty()) {
            fontRenderer.drawString(label, x + boxSize + LABEL_X_OFFSET, labelY, labelColor);
        }

        if (description != null && !description.isEmpty()) {
            int descY = labelY + fontRenderer.FONT_HEIGHT + DESCRIPTION_GAP;
            int descColor = enabled ? GuiColors.TEXT_SECONDARY : GuiColors.TEXT_DISABLED;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + boxSize + LABEL_X_OFFSET, descY, 0f);
            GlStateManager.scale(DESCRIPTION_SCALE, DESCRIPTION_SCALE, 1f);
            fontRenderer.drawString(description, 0, 0, descColor);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int mouseButton) {
        if (enabled && visible && mouseButton == 0) {
            boolean boxClicked = mouseX >= this.x && mouseX < this.x + boxSize
                    && mouseY >= this.y && mouseY < this.y + boxSize;
            boolean labelClicked = onLabel(mouseX, mouseY);

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

    public String description() {
        return description;
    }

    private void resize() {
        int h = boxSize;
        if (description != null && !description.isEmpty()) {
            int descLineH = Math.max(1, Math.round(fontRenderer.FONT_HEIGHT * DESCRIPTION_SCALE));
            int labelBottom = Math.max(boxSize, (boxSize - fontRenderer.FONT_HEIGHT) / 2 + 1 + fontRenderer.FONT_HEIGHT);
            h = Math.max(h, labelBottom + DESCRIPTION_GAP + descLineH);
        }
        this.height = h;
        this.width = boxSize;
    }

    private boolean onLabel(int mouseX, int mouseY) {
        if (label == null || label.isEmpty()) {
            return false;
        }
        int labelStartX = x + boxSize + LABEL_X_OFFSET;
        int labelRenderY = y + (boxSize - fontRenderer.FONT_HEIGHT) / 2 + 1;
        int labelTextWidth = fontRenderer.getStringWidth(label);
        int labelBottom = labelRenderY + fontRenderer.FONT_HEIGHT;
        if (description != null && !description.isEmpty()) {
            int descLineH = Math.max(1, Math.round(fontRenderer.FONT_HEIGHT * DESCRIPTION_SCALE));
            int descWidth = Math.round(fontRenderer.getStringWidth(description) * DESCRIPTION_SCALE);
            labelTextWidth = Math.max(labelTextWidth, descWidth);
            labelBottom = labelRenderY + fontRenderer.FONT_HEIGHT + DESCRIPTION_GAP + descLineH;
        }
        return mouseX >= labelStartX && mouseX < labelStartX + labelTextWidth
                && mouseY >= labelRenderY && mouseY < labelBottom;
    }

    public interface OnValueChanged {
        void accept(boolean value);
    }
}