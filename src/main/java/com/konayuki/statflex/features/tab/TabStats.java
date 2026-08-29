package com.konayuki.statflex.features.tab;

import com.konayuki.statflex.features.tab.TabStatsCache.Snapshot;
import com.konayuki.statflex.utils.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TabStats {
    private static final int TOP_Y = 20;
    private static final int BORDER = 3;
    private static final int PADDING_X = 6;
    private static final int ROW_PAD_Y = 2;
    private static final int HEADER_PAD_Y = 3;
    private static final int ROW_GAP = 1;

    private static final int FRAME_COLOR = Integer.MIN_VALUE;
    private static final int HEADER_BG = 0x30FFFFFF;
    private static final int ROW_BG = 553648127;
    private static final int FOOTER_BG = 0x40000000;
    private static final int INDICATOR_COLOR = 0xFFAAAAAA;

    private final Minecraft mc = Minecraft.getMinecraft();
    private net.minecraft.world.World lastWorld;

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            return;
        }
        if (!isActive()) {
            return;
        }
        event.setCanceled(true);
        render();
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.dwheel == 0 || !isActive()) {
            return;
        }
        event.setCanceled(true);
        int direction = event.dwheel > 0 ? -1 : 1;
        TabStatsCache.setScrollIndex(TabStatsCache.scrollIndex() + direction);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (mc.theWorld != lastWorld) {
            lastWorld = mc.theWorld;
            TabStatsCache.clear();
        }
    }

    private boolean isActive() {
        Snapshot snapshot = TabStatsCache.get();
        if (snapshot == null || !TabStatsCache.isTabMode(snapshot.game)) {
            return false;
        }
        if (!Toggle.isAllowed()) {
            return false;
        }
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings == null) {
            return false;
        }
        return mc.gameSettings.keyBindPlayerList.isKeyDown();
    }

    private void render() {
        Snapshot snapshot = TabStatsCache.get();
        if (snapshot == null || (snapshot.lines.isEmpty() && snapshot.footer == null)) {
            return;
        }

        ScaledResolution scaled = new ScaledResolution(mc);
        FontRenderer font = mc.fontRendererObj;
        int screenW = scaled.getScaledWidth();
        int screenH = scaled.getScaledHeight();

        int rowHeight = font.FONT_HEIGHT + ROW_PAD_Y * 2;
        int headerHeight = font.FONT_HEIGHT + HEADER_PAD_Y * 2;

        int contentWidth = font.getStringWidth(snapshot.header);
        for (String line : snapshot.lines) {
            contentWidth = Math.max(contentWidth, font.getStringWidth(line));
        }
        if (snapshot.footer != null) {
            contentWidth = Math.max(contentWidth, font.getStringWidth(snapshot.footer));
        }
        int panelWidth = contentWidth + PADDING_X * 2;

        float fitScale = 1f;
        int availableWidth = screenW - BORDER * 2;
        if (panelWidth > availableWidth) {
            fitScale = availableWidth / (float) panelWidth;
            panelWidth = availableWidth;
        }

        int total = snapshot.lines.size();
        int footerHeight = snapshot.footer != null ? rowHeight : 0;
        int availableHeight = screenH - TOP_Y * 2 - headerHeight - footerHeight - BORDER * 2;
        int rowStep = rowHeight + ROW_GAP;
        int maxVisible = Math.max(1, availableHeight / rowStep);

        int scroll = TabStatsCache.scrollIndex();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - maxVisible)));
        TabStatsCache.setScrollIndex(scroll);
        int visible = Math.min(maxVisible, total - scroll);
        boolean scrollable = total > maxVisible;

        int panelHeight = headerHeight + visible * rowStep;
        if (snapshot.footer != null) {
            panelHeight += ROW_GAP + footerHeight;
        }
        if (scrollable) {
            panelHeight += ROW_GAP + rowHeight;
        }

        int startX = (screenW - panelWidth) / 2;
        int startY = TOP_Y;

        GlStateManager.pushMatrix();
        GlStateManager.translate(startX, startY, 0f);
        GlStateManager.scale(fitScale, fitScale, 1f);

        net.minecraft.client.gui.Gui.drawRect(-BORDER, -BORDER, panelWidth + BORDER, panelHeight + BORDER, FRAME_COLOR);
        net.minecraft.client.gui.Gui.drawRect(0, 0, panelWidth, headerHeight, HEADER_BG);
        font.drawStringWithShadow(snapshot.header, (panelWidth - font.getStringWidth(snapshot.header)) / 2f,
                HEADER_PAD_Y, 0xFFFFFF);

        int rowY = headerHeight + ROW_GAP;
        for (int i = scroll; i < scroll + visible; i++) {
            net.minecraft.client.gui.Gui.drawRect(0, rowY, panelWidth, rowY + rowHeight, ROW_BG);
            font.drawStringWithShadow(snapshot.lines.get(i), PADDING_X, rowY + ROW_PAD_Y, 0xFFFFFF);
            rowY += rowStep;
        }

        if (snapshot.footer != null) {
            net.minecraft.client.gui.Gui.drawRect(0, rowY, panelWidth, rowY + footerHeight, FOOTER_BG);
            String footer = font.trimStringToWidth(snapshot.footer, panelWidth - PADDING_X * 2);
            font.drawStringWithShadow(footer, PADDING_X, rowY + ROW_PAD_Y, 0xFFFFFF);
            rowY += footerHeight + ROW_GAP;
        }

        if (scrollable) {
            String indicator = "\u25B2 \u25BC  " + (scroll + 1) + "-" + (scroll + visible) + " / " + total;
            indicator = font.trimStringToWidth(indicator, panelWidth - PADDING_X * 2);
            font.drawStringWithShadow(indicator, panelWidth - font.getStringWidth(indicator) - PADDING_X,
                    rowY + ROW_PAD_Y, INDICATOR_COLOR);
        }

        GlStateManager.popMatrix();
    }
}
