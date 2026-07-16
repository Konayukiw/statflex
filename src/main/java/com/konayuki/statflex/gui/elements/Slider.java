package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Locale;

public class Slider extends GuiComponentBase {

    public interface OnValueChanged {
        void accept(float value);
    }

    public interface DisplayFormat {
        String format(float value);
    }

    private float currentValue;
    private boolean isDragging;
    private final float minValue;
    private final float maxValue;
    private final float step;
    private final DisplayFormat displayFormat;
    private final OnValueChanged onValueChanged;

    private final float knobVisualRadius = 6f;
    private final float trackHeightToUse = MODERN_SLIDER_TRACK_HEIGHT;
    private final float trackCornerRadius = trackHeightToUse / 2f;

    private float visualKnobCenterX;
    private float targetKnobCenterX;
    private static final float KNOB_SMOOTH_FACTOR = 0.25f;

    public Slider(int id, int x, int y, int width, int height, String label,
                  float initialValue, float minValue, float maxValue, float step,
                  DisplayFormat displayFormat, OnValueChanged onValueChanged) {
        super(id, x, y, width, height, label);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.displayFormat = displayFormat != null
                ? displayFormat
                : v -> String.format(Locale.US, "%.2f", v);
        this.onValueChanged = onValueChanged;
        internalSetValue(initialValue, false);
        targetKnobCenterX = calculateKnobCenterX(this.currentValue);
        visualKnobCenterX = targetKnobCenterX;
    }

    public Slider(int id, int x, int y, int width, String label,
                  float initialValue, float minValue, float maxValue, float step,
                  DisplayFormat displayFormat, OnValueChanged onValueChanged) {
        this(id, x, y, width, MODERN_SLIDER_HEIGHT, label,
                initialValue, minValue, maxValue, step, displayFormat, onValueChanged);
    }

    private float calculateKnobCenterX(float value) {
        float progress = (maxValue - minValue == 0f) ? 0f : (value - minValue) / (maxValue - minValue);
        float travelWidth = this.width - (2 * knobVisualRadius);
        return this.x + knobVisualRadius + (travelWidth > 0 ? travelWidth * progress : 0f);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY, float partialTicks) {
        super.drawComponent(mouseX, mouseY, partialTicks);
        if (!visible) {
            return;
        }

        float diff = targetKnobCenterX - visualKnobCenterX;
        if (Math.abs(diff) > 0.001f) {
            visualKnobCenterX = visualKnobCenterX + diff * KNOB_SMOOTH_FACTOR;
        } else {
            visualKnobCenterX = targetKnobCenterX;
        }

        float minKnobX = x + knobVisualRadius;
        float maxKnobX = x + width - knobVisualRadius;
        float currentKnobRenderCenterX = Math.max(minKnobX, Math.min(maxKnobX, visualKnobCenterX));
        float knobRenderY = y + height / 2f;

        drawTopLabel(-3);
        String valueText = displayFormat.format(currentValue);
        int valueColor = enabled ? GuiColors.TEXT_ACCENT : GuiColors.TEXT_DISABLED;
        int valueTextWidth = fontRenderer.getStringWidth(valueText);
        fontRenderer.drawString(
                valueText,
                x + width - valueTextWidth,
                y - fontRenderer.FONT_HEIGHT - 7,
                valueColor
        );

        float trackActualY = y + (height - trackHeightToUse) / 2f;
        int trackColor = enabled ? GuiColors.SLIDER_TRACK : GuiColors.COMPONENT_BACKGROUND_DISABLED;
        drawRoundedRectUsingGL(x, trackActualY, width, trackHeightToUse, trackCornerRadius, trackColor);

        float filledWidth = currentKnobRenderCenterX - x;
        if (filledWidth > 0f) {
            int filledTrackColor = enabled ? GuiColors.SLIDER_TRACK_FILLED : GuiColors.PRIMARY_BLUE_DARK;
            drawRoundedRectUsingGL(
                    x, trackActualY,
                    Math.min(filledWidth, (float) width), trackHeightToUse,
                    trackCornerRadius, filledTrackColor
            );
        }

        if (knobVisualRadius <= 0f) {
            return;
        }

        boolean isHoveringKnob = mouseX >= currentKnobRenderCenterX - knobVisualRadius
                && mouseX <= currentKnobRenderCenterX + knobVisualRadius
                && mouseY >= knobRenderY - knobVisualRadius
                && mouseY <= knobRenderY + knobVisualRadius;

        int knobColor;
        if (!enabled) {
            knobColor = GuiColors.PRIMARY_BLUE_DARK;
        } else if (isDragging || isHoveringKnob) {
            knobColor = GuiColors.PRIMARY_BLUE_BRIGHT;
        } else {
            knobColor = GuiColors.PRIMARY_BLUE;
        }

        drawCircleUsingGL(currentKnobRenderCenterX, knobRenderY, knobVisualRadius, knobColor);
    }

    private void internalSetValue(float newValue, boolean notify) {
        float oldValue = this.currentValue;
        float tempValue = Math.max(minValue, Math.min(maxValue, newValue));
        if (step > 0f) {
            int decimalPlaces = getDecimalPlaces(step);
            tempValue = Math.round(tempValue / step) * step;
            tempValue = Float.parseFloat(String.format(Locale.US, "%." + decimalPlaces + "f", tempValue));
        }
        this.currentValue = Math.max(minValue, Math.min(maxValue, tempValue));
        this.targetKnobCenterX = calculateKnobCenterX(this.currentValue);
        float threshold = Math.min(step / 2.0f, 0.00001f);
        if (notify && Math.abs(oldValue - this.currentValue) > threshold && onValueChanged != null) {
            onValueChanged.accept(this.currentValue);
        }
    }

    public void setValue(float newValue) {
        internalSetValue(newValue, true);
        visualKnobCenterX = calculateKnobCenterX(this.currentValue);
        targetKnobCenterX = visualKnobCenterX;
    }

    public float getValue() {
        return currentValue;
    }

    private int getDecimalPlaces(float value) {
        String s = String.valueOf(value).replace(',', '.');
        int dot = s.indexOf('.');
        return dot < 0 ? 0 : s.length() - dot - 1;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!enabled || !visible || !super.mouseClicked(mouseX, mouseY, mouseButton)) {
            return false;
        }

        if (mouseButton == 0) {
            isDragging = true;
            updateValueFromMouse(mouseX, true);
            visualKnobCenterX = calculateKnobCenterX(this.currentValue);
            targetKnobCenterX = visualKnobCenterX;
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 0.6F));
            return true;
        }
        return false;
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDragging && clickedMouseButton == 0 && enabled) {
            updateValueFromMouse(mouseX, true);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && isDragging) {
            isDragging = false;
        }
    }

    private void updateValueFromMouse(int mouseX, boolean notify) {
        if (!enabled) {
            return;
        }
        float travelWidth = this.width - (2 * knobVisualRadius);
        if (travelWidth <= 0f) {
            return;
        }
        float relativeMouseX = mouseX - (this.x + knobVisualRadius);
        float ratio = Math.max(0f, Math.min(1f, relativeMouseX / travelWidth));
        float newValue = minValue + (maxValue - minValue) * ratio;
        internalSetValue(newValue, notify);
    }

    private void drawCircleUsingGL(float cx, float cy, float radius, int colorInt) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        Color awtColor = new Color(colorInt, true);
        GlStateManager.color(
                awtColor.getRed() / 255.0f,
                awtColor.getGreen() / 255.0f,
                awtColor.getBlue() / 255.0f,
                awtColor.getAlpha() / 255.0f
        );

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        int segments = 30;
        for (int i = 0; i <= segments; i++) {
            float angle = (i / (float) segments) * (float) (Math.PI * 2.0);
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
