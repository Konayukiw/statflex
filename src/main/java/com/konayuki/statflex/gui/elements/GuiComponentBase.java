package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import com.konayuki.statflex.gui.GuiFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public abstract class GuiComponentBase {

    public static final float DEFAULT_CORNER_RADIUS = 4f;
    public static final float DEFAULT_BORDER_THICKNESS = 1f;
    public static final float MODERN_CORNER_RADIUS = 5f;
    public static final float MODERN_BORDER_THICKNESS = 1f;
    public static final int MODERN_ELEMENT_PADDING_X = 10;
    public static final float MODERN_ELEMENT_PADDING_Y_RATIO = 0.3f;
    public static final int MODERN_TEXT_INPUT_HEIGHT = 26;
    public static final int MODERN_BUTTON_HEIGHT = 28;
    public static final int MODERN_DROPDOWN_HEIGHT = 26;
    public static final int MODERN_CHECKBOX_SIZE = 16;
    public static final int MODERN_SLIDER_HEIGHT = 20;
    public static final float MODERN_SLIDER_KNOB_RADIUS = 7f;
    public static final float MODERN_SLIDER_TRACK_HEIGHT = 5f;

    public static final float SHADOW_OFFSET_X = 1f;
    public static final float SHADOW_OFFSET_Y = 1f;

    public int id;
    public int x;
    public int y;
    public int width;
    public int height;
    public String label;

    protected final Minecraft mc = Minecraft.getMinecraft();
    protected final FontRenderer fontRenderer = GuiFonts.getInstance();

    public boolean enabled = true;
    public boolean visible = true;
    public boolean hovered = false;

    public GuiComponentBase(int id, int x, int y, int width, int height, String label) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label != null ? label : "";
    }

    public void drawComponent(int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        this.hovered = enabled
                && mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!enabled || !visible) {
            return false;
        }
        return mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        return false;
    }

    protected void drawSideLabel(int labelYOffset, int xOffset) {
        if (label != null && !label.isEmpty()) {
            int labelColor = enabled ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_DISABLED;
            fontRenderer.drawString(label, x + width + xOffset, y + labelYOffset, labelColor);
        }
    }

    protected void drawTopLabel(int yOffset) {
        if (label != null && !label.isEmpty()) {
            int labelColor = enabled ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_DISABLED;
            fontRenderer.drawString(label, x, y - fontRenderer.FONT_HEIGHT + yOffset, labelColor);
        }
    }

    protected void drawRoundedRectUsingGL(float x, float y, float width, float height, float radius, int colorInt) {
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
        GL11.glBegin(GL11.GL_POLYGON);
        int segments = 20;
        float pi = (float) Math.PI;
        for (int i = 0; i <= segments; i++) {
            float angle = (i / (float) segments) * (pi / 2f);
            GL11.glVertex2f(x + width - radius + (float) Math.cos(angle) * radius,
                    y + height - radius + (float) Math.sin(angle) * radius);
        }
        for (int i = 0; i <= segments; i++) {
            float angle = (pi / 2f) + (i / (float) segments) * (pi / 2f);
            GL11.glVertex2f(x + radius + (float) Math.cos(angle) * radius,
                    y + height - radius + (float) Math.sin(angle) * radius);
        }
        for (int i = 0; i <= segments; i++) {
            float angle = pi + (i / (float) segments) * (pi / 2f);
            GL11.glVertex2f(x + radius + (float) Math.cos(angle) * radius,
                    y + radius + (float) Math.sin(angle) * radius);
        }
        for (int i = 0; i <= segments; i++) {
            float angle = (1.5f * pi) + (i / (float) segments) * (pi / 2f);
            GL11.glVertex2f(x + width - radius + (float) Math.cos(angle) * radius,
                    y + radius + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}