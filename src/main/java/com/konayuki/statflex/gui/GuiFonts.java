package com.konayuki.statflex.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class GuiFonts extends FontRenderer {

    public static final float DEFAULT_FONT_SIZE = 11f;
    public static final float TITLE_FONT_SIZE = 15f;
    private static final int RASTER_SCALE = 2;
    private static final int LETTER_SPACING = 1;

    private static GuiFonts instance;
    private static GuiFonts titleInstance;

    public static GuiFonts getInstance() {
        if (instance == null) {
            instance = new GuiFonts("/assets/fonts/Celik-Display-Pro-Black.ttf", DEFAULT_FONT_SIZE);
        }
        return instance;
    }

    public static GuiFonts getTitle() {
        if (titleInstance == null) {
            titleInstance = new GuiFonts("/assets/fonts/D-DIN-PRO-800.ttf", TITLE_FONT_SIZE);
        }
        return titleInstance;
    }

    private static final class Glyph {
        final int textureId;
        final float width;
        final float offsetX;
        final float offsetY;
        final float texWidth;
        final float texHeight;

        Glyph(int textureId, float width, float offsetX, float offsetY, float texWidth, float texHeight) {
            this.textureId = textureId;
            this.width = width;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.texWidth = texWidth;
            this.texHeight = texHeight;
        }
    }

    private final Font font;
    private final float fontSize;
    private final FontMetrics metrics;
    private final Font fallbackFont;
    private final FontMetrics fallbackMetrics;
    private final int ascent;
    private final int descent;
    private final Map<Character, Glyph> glyphCache = new HashMap<Character, Glyph>();

    public GuiFonts(String fontResource, float fontSize) {
        super(
                Minecraft.getMinecraft().gameSettings,
                new ResourceLocation("textures/font/ascii.png"),
                Minecraft.getMinecraft().getTextureManager(),
                false
        );

        this.fontSize = fontSize;

        Font loaded = null;
        try {
            InputStream stream = GuiFonts.class.getResourceAsStream(fontResource);
            if (stream == null) {
            }
            try {
                loaded = Font.createFont(Font.TRUETYPE_FONT, stream);
            } finally {
                stream.close();
            }
        } catch (Exception e) {
        }
        this.font = loaded.deriveFont(fontSize);

        this.fallbackFont = new Font(Font.SANS_SERIF, Font.PLAIN, 1).deriveFont(fontSize);

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = probe.createGraphics();
        try {
            graphics.setFont(this.font);
            this.metrics = graphics.getFontMetrics(this.font);
            this.fallbackMetrics = graphics.getFontMetrics(this.fallbackFont);
        } finally {
            graphics.dispose();
        }
        this.ascent = this.metrics.getAscent();
        this.descent = this.metrics.getDescent();
        int leading = Math.max(0, this.metrics.getLeading());
        this.FONT_HEIGHT = Math.max(1, (int) Math.ceil(this.ascent + this.descent + leading) + 1);
    }

    @Override
    public int drawString(String text, int x, int y, int color) {
        return drawString(text, (float) x, (float) y, color, false);
    }

    @Override
    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null) {
            text = "";
        }
        if (dropShadow) {
            renderText(text, x + 1f, y + 1f, (color & 0x3F3F3F) | 0xFF000000);
        }
        return renderText(text, x, y, color);
    }

    @Override
    public int drawStringWithShadow(String text, float x, float y, int color) {
        return drawString(text, x, y, color, true);
    }

    @Override
    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        boolean skipNext = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\u00a7') {
                skipNext = true;
            } else if (skipNext) {
                skipNext = false;
            } else if (ch != '\n' && ch != '\r') {
                width += getGlyph(ch).width;
            }
        }
        return Math.round(width);
    }

    @Override
    public int getCharWidth(char character) {
        return Math.round(getGlyph(character).width);
    }

    @Override
    public String trimStringToWidth(String text, int width) {
        return trimStringToWidth(text, width, false);
    }

    @Override
    public String trimStringToWidth(String text, int width, boolean reverse) {
        if (text == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int currentWidth = 0;
        boolean skipNext = false;
        int start = reverse ? text.length() - 1 : 0;
        int end = reverse ? -1 : text.length();
        int step = reverse ? -1 : 1;
        for (int i = start; i != end; i += step) {
            char ch = text.charAt(i);
            if (ch == '\u00a7') {
                skipNext = true;
                continue;
            }
            if (skipNext) {
                skipNext = false;
                continue;
            }
            int charWidth = Math.round(getGlyph(ch).width);
            if (currentWidth + charWidth > width) {
                break;
            }
            currentWidth += charWidth;
            if (reverse) {
                result.insert(0, ch);
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private int renderText(String text, float x, float y, int color) {
        if (text.length() == 0) {
            return (int) x;
        }

        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();

        int alpha = (color >>> 24) == 0 ? 0xFF : (color >>> 24);
        Color awtColor = new Color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
        GlStateManager.color(
                awtColor.getRed() / 255.0f,
                awtColor.getGreen() / 255.0f,
                awtColor.getBlue() / 255.0f,
                awtColor.getAlpha() / 255.0f
        );

        float penX = x;
        float baseline = y + ascent;
        boolean skipNext = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\u00a7') {
                skipNext = true;
                continue;
            }
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (ch == '\n' || ch == '\r') {
                continue;
            }
            Glyph glyph = getGlyph(ch);
            if (glyph.textureId >= 0) {
                float gx = penX + glyph.offsetX;
                float gy = baseline + glyph.offsetY;
                float gw = glyph.texWidth / RASTER_SCALE;
                float gh = glyph.texHeight / RASTER_SCALE;

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, glyph.textureId);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0f, 0f);
                GL11.glVertex2f(gx, gy);
                GL11.glTexCoord2f(1f, 0f);
                GL11.glVertex2f(gx + gw, gy);
                GL11.glTexCoord2f(1f, 1f);
                GL11.glVertex2f(gx + gw, gy + gh);
                GL11.glTexCoord2f(0f, 1f);
                GL11.glVertex2f(gx, gy + gh);
                GL11.glEnd();
            }
            penX += glyph.width;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        return (int) penX;
    }

    private Glyph getGlyph(char character) {
        Glyph glyph = glyphCache.get(Character.valueOf(character));
        if (glyph == null) {
            glyph = rasterize(character);
            glyphCache.put(Character.valueOf(character), glyph);
        }
        return glyph;
    }

    private Glyph rasterize(char character) {
        if (character == ' ') {
            return new Glyph(-1, metrics.charWidth(' ') + LETTER_SPACING, 0f, 0f, 0f, 0f);
        }

        float rasterSize = fontSize * RASTER_SCALE;
        int cellWidth = Math.max(8, (int) Math.ceil(rasterSize * 1.8f));
        int cellHeight = Math.max(8, (int) Math.ceil((FONT_HEIGHT + 2) * RASTER_SCALE));
        int baseline = (int) Math.ceil(ascent * RASTER_SCALE) + RASTER_SCALE;

        BufferedImage cell = new BufferedImage(cellWidth, cellHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cell.createGraphics();
        boolean useFallback = !font.canDisplay(character);
        Font drawFont = useFallback ? fallbackFont.deriveFont(rasterSize) : font.deriveFont(rasterSize);
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setFont(drawFont);
            graphics.setColor(Color.WHITE);
            graphics.drawString(String.valueOf(character), RASTER_SCALE * 2, baseline);
        } finally {
            graphics.dispose();
        }

        int[] pixels = cell.getRGB(0, 0, cellWidth, cellHeight, null, 0, cellWidth);
        int minX = cellWidth, minY = cellHeight, maxX = -1, maxY = -1;
        for (int y = 0; y < cellHeight; y++) {
            for (int x = 0; x < cellWidth; x++) {
                if (((pixels[y * cellWidth + x] >>> 24) & 0xFF) > 8) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < 0) {
            FontMetrics emptyMetrics = useFallback ? fallbackMetrics : metrics;
            return new Glyph(-1, emptyMetrics.charWidth(character) + LETTER_SPACING, 0f, 0f, 0f, 0f);
        }

        int pad = 1;
        int cropLeft = Math.max(0, minX - pad);
        int cropTop = Math.max(0, minY - pad);
        int cropRight = Math.min(cellWidth - 1, maxX + pad);
        int cropBottom = Math.min(cellHeight - 1, maxY + pad);
        int texWidth = cropRight - cropLeft + 1;
        int texHeight = cropBottom - cropTop + 1;

        ByteBuffer data = ByteBuffer.allocateDirect(texWidth * texHeight * 4);
        for (int y = 0; y < texHeight; y++) {
            for (int x = 0; x < texWidth; x++) {
                int alpha = (pixels[(y + cropTop) * cellWidth + (x + cropLeft)] >>> 24) & 0xFF;
                data.put((byte) alpha);
                data.put((byte) alpha);
                data.put((byte) alpha);
                data.put((byte) alpha);
            }
        }
        data.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, texWidth, texHeight, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        float advance;
        if (font.canDisplay(character)) {
            advance = metrics.charWidth(character) + LETTER_SPACING;
        } else if (fallbackFont.canDisplay(character)) {
            advance = fallbackMetrics.charWidth(character) + LETTER_SPACING;
        } else {
            advance = texWidth / (float) RASTER_SCALE + 1f + LETTER_SPACING;
        }
        float offsetX = (cropLeft - RASTER_SCALE * 2) / (float) RASTER_SCALE;
        float offsetY = (cropTop - baseline) / (float) RASTER_SCALE;
        return new Glyph(textureId, advance, offsetX, offsetY, texWidth, texHeight);
    }
}
