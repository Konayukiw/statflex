package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.gui.GuiTextField;

public class Color extends GuiComponentBase {

    public interface OnColorChanged {
        void accept(int argb);
    }

    private static final int SWATCH_SIZE = 18;
    private static final int FIELD_HEIGHT = MODERN_TEXT_INPUT_HEIGHT;
    private static final int GAP = 6;

    private final String colorKey;
    private final OnColorChanged onColorChanged;
    private final GuiTextField hexField;
    private int currentColor;
    private final float cornerRadius = MODERN_CORNER_RADIUS;

    public Color(int id, int x, int y, int width, String colorKey, String displayName,
                 int initialColor, OnColorChanged onColorChanged) {
        super(id, x, y, width, fontHeight() + 3 + FIELD_HEIGHT, displayName);
        this.colorKey = colorKey;
        this.onColorChanged = onColorChanged;
        this.currentColor = initialColor;

        int fieldX = x + SWATCH_SIZE + GAP;
        int fieldW = Math.max(40, width - SWATCH_SIZE - GAP);
        int fieldY = y + fontHeight() + 3;

        hexField = new GuiTextField(id, fontRenderer, fieldX + 4, fieldY + (FIELD_HEIGHT - fontRenderer.FONT_HEIGHT) / 2,
                fieldW - 8, fontRenderer.FONT_HEIGHT);
        hexField.setMaxStringLength(7);
        hexField.setEnableBackgroundDrawing(false);
        hexField.setTextColor(GuiColors.TEXTFIELD_TEXT);
        hexField.setText("#" + GuiColors.toHexRgb(initialColor));
        hexField.setFocused(false);
    }

    private static int fontHeight() {
        return net.minecraft.client.Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
    }

    public String getColorKey() {
        return colorKey;
    }

    public int getColor() {
        return currentColor;
    }

    public void setColor(int argb, boolean notify) {
        this.currentColor = argb;
        String hex = "#" + GuiColors.toHexRgb(argb);
        if (!hex.equalsIgnoreCase(hexField.getText())) {
            hexField.setText(hex);
        }
        if (notify && onColorChanged != null) {
            onColorChanged.accept(argb);
        }
    }

    public boolean isFocused() {
        return hexField.isFocused();
    }

    public void setFocused(boolean focused) {
        hexField.setFocused(focused);
    }

    public void unfocusIfNeeded() {
        if (hexField.isFocused()) {
            hexField.setFocused(false);
            commitHexText();
        }
    }

    @Override
    public void drawComponent(int mouseX, int mouseY, float partialTicks) {
        super.drawComponent(mouseX, mouseY, partialTicks);
        if (!visible) {
            return;
        }

        int labelColor = enabled ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_DISABLED;
        fontRenderer.drawString(label, x, y, labelColor);

        int swatchY = y + fontRenderer.FONT_HEIGHT + 3;
        int fieldX = x + SWATCH_SIZE + GAP;
        int fieldW = Math.max(40, width - SWATCH_SIZE - GAP);

        drawRoundedRectUsingGL(x, swatchY, SWATCH_SIZE, SWATCH_SIZE, 3f, GuiColors.COMPONENT_BORDER);
        drawRoundedRectUsingGL(x + 1f, swatchY + 1f, SWATCH_SIZE - 2f, SWATCH_SIZE - 2f, 2f, currentColor);

        int borderColor = hexField.isFocused() && enabled
                ? GuiColors.TEXTFIELD_BORDER_FOCUSED
                : GuiColors.TEXTFIELD_BORDER;
        drawRoundedRectUsingGL(fieldX, swatchY, fieldW, FIELD_HEIGHT, cornerRadius, borderColor);
        drawRoundedRectUsingGL(
                fieldX + 1f, swatchY + 1f,
                fieldW - 2f, FIELD_HEIGHT - 2f,
                Math.max(0f, cornerRadius - 1f),
                GuiColors.TEXTFIELD_BACKGROUND
        );

        hexField.xPosition = fieldX + 4;
        hexField.yPosition = swatchY + (FIELD_HEIGHT - fontRenderer.FONT_HEIGHT) / 2;
        hexField.width = fieldW - 8;
        hexField.setEnabled(enabled);
        hexField.setTextColor(enabled ? GuiColors.TEXTFIELD_TEXT : GuiColors.TEXT_DISABLED);
        hexField.drawTextBox();
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!visible || !enabled) {
            if (hexField.isFocused()) {
                setFocused(false);
            }
            return false;
        }

        int swatchY = y + fontRenderer.FONT_HEIGHT + 3;
        int fieldX = x + SWATCH_SIZE + GAP;
        int fieldW = Math.max(40, width - SWATCH_SIZE - GAP);
        boolean onField = mouseX >= fieldX && mouseX < fieldX + fieldW
                && mouseY >= swatchY && mouseY < swatchY + FIELD_HEIGHT;
        boolean onSwatch = mouseX >= x && mouseX < x + SWATCH_SIZE
                && mouseY >= swatchY && mouseY < swatchY + SWATCH_SIZE;
        boolean onLabel = mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + fontRenderer.FONT_HEIGHT + 3 + FIELD_HEIGHT;

        if (onField || onSwatch) {
            hexField.mouseClicked(mouseX, mouseY, mouseButton);
            if (onSwatch && !hexField.isFocused()) {
                hexField.setFocused(true);
            }
            return true;
        }

        if (hexField.isFocused()) {
            setFocused(false);
        }
        return onLabel;
    }

    @Override
    public boolean keyTyped(char typedChar, int keyCode) {
        if (!enabled || !visible || !hexField.isFocused()) {
            return false;
        }

        boolean handled = hexField.textboxKeyTyped(typedChar, keyCode);
        if (handled) {
            commitHexText();
        }
        return handled;
    }

    private void commitHexText() {
        String raw = hexField.getText();
        Integer parsed = GuiColors.parseHexRgb(raw);
        if (parsed != null && parsed != currentColor) {
            currentColor = parsed;
            if (onColorChanged != null) {
                onColorChanged.accept(currentColor);
            }
            String normalized = "#" + GuiColors.toHexRgb(currentColor);
            if (!normalized.equals(hexField.getText())) {
                hexField.setText(normalized);
            }
        }
    }
}