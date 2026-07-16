package com.konayuki.statflex.gui;

import com.konayuki.statflex.gui.elements.Button;
import com.konayuki.statflex.gui.elements.Checkbox;
import com.konayuki.statflex.gui.elements.Dropdown;
import com.konayuki.statflex.gui.elements.GuiComponentBase;
import com.konayuki.statflex.gui.elements.Slider;
import com.konayuki.statflex.gui.elements.Text;
import com.konayuki.statflex.utils.HypixelApiUtil;
import com.konayuki.statflex.utils.Settings;
import com.konayuki.statflex.utils.Toggles;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Config GUI ported from WLR ConfigGui, wired to statflex Settings / Toggles.
 */

public class ConfigGui extends GuiScreen {

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private final float panelCornerRadius = 8f;
    private final int topBarHeight = 35;
    private final int tabBarButtonHeight = 28;
    private final int panelPadding = 15;
    private final int closeButtonSize = 18;
    private boolean isCloseButtonHovered;

    private static class Tab {
        final String name;
        final String id;
        final List<GuiComponentBase> components = new ArrayList<GuiComponentBase>();
        float scrollY;
        int targetScrollY;
        int contentHeight;
        int maxScrollY;

        Tab(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }

    private final List<Tab> tabs = new ArrayList<Tab>();
    private int currentTabIndex = 0;

    private Tab currentTab() {
        if (tabs.isEmpty()) {
            return new Tab("Error", "error_no_tabs");
        }
        if (currentTabIndex < 0 || currentTabIndex >= tabs.size()) {
            return tabs.get(0);
        }
        return tabs.get(currentTabIndex);
    }

    private final String guiTitle = "statflex Settings";
    private final int tabButtonWidth = 90;

    private float tabScrollX;
    private int targetTabScrollX;
    private int totalTabsWidthUnscrolled;
    private int visibleTabBarAreaWidth;
    private int maxTabScrollX;
    private final int tabButtonSpacing = 4;
    private final int tabBarScrollButtonWidth = 20;

    private final int scrollbarWidth = 8;
    private final int scrollbarMargin = 5;
    private static final float SCROLL_SMOOTHING_FACTOR = 0.28f;

    private boolean isDraggingContentScrollbar;
    private float contentScrollbarMouseDragStartY;
    private float contentScrollbarInitialScrollY;
    private Dropdown openDropdown;

    private int logicalCurrentY;
    private final int interComponentSpacing = 12;
    private final int componentWidth = 240;
    private int labelHeightAboveComponent;
    private final int contentPaddingTopForComponents = 15;
    private final int contentPaddingBottomForComponents = 15;
    private final Map<String, Tab> allPossibleTabsMap = new LinkedHashMap<String, Tab>();

    private int nextComponentId = 1;

    private int getNextId() {
        return nextComponentId++;
    }

    @Override
    public void initGui() {
        super.initGui();
        panelWidth = Math.min(800, this.width - 60);
        panelHeight = Math.min(550, this.height - 60);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        this.labelHeightAboveComponent = mc.fontRendererObj.FONT_HEIGHT + 3;
        nextComponentId = 1;
        Keyboard.enableRepeatEvents(true);
        openDropdown = null;
        tabScrollX = 0f;
        targetTabScrollX = 0;
        isDraggingContentScrollbar = false;

        try {
            defineAllPossibleTabs();
            orderAndPopulateTabs();

            if (this.tabs.isEmpty()) {
                return;
            }
            if (currentTabIndex >= tabs.size()) {
                currentTabIndex = 0;
            }

            calculateTabScrolling();
            for (Tab tab : tabs) {
                calculateContentScrollingForTab(tab);
                tab.scrollY = 0f;
                tab.targetScrollY = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void defineAllPossibleTabs() {
        allPossibleTabsMap.clear();
        allPossibleTabsMap.put("Toggles", new Tab("Toggles", "Toggles"));
        allPossibleTabsMap.put("General", new Tab("General", "General"));
        allPossibleTabsMap.put("Warnings", new Tab("Warnings", "Warnings"));
        allPossibleTabsMap.put("AutoGG", new Tab("AutoGG", "AutoGG"));
    }

    private void orderAndPopulateTabs() {
        tabs.clear();
        String[] order = {"Toggles", "General", "Warnings", "AutoGG"};
        for (String tabId : order) {
            Tab tab = allPossibleTabsMap.get(tabId);
            if (tab != null) {
                tab.components.clear();
                populateComponentsForTab(tab);
                tabs.add(tab);
            }
        }
        for (Tab tab : allPossibleTabsMap.values()) {
            if (!tabs.contains(tab)) {
                tab.components.clear();
                populateComponentsForTab(tab);
                tabs.add(tab);
            }
        }
    }

    private void populateComponentsForTab(Tab tab) {
        Settings settings = Settings.getInstance();
        int contentAreaWidth = panelWidth - (panelPadding * 2);
        int actualComponentWidth = Math.min(this.componentWidth, contentAreaWidth);
        int startX = panelX + panelPadding + (contentAreaWidth - actualComponentWidth) / 2;

        logicalCurrentY = 0;
        logicalCurrentY += contentPaddingTopForComponents;

        if ("Toggles".equals(tab.id)) {
            addCheckbox(tab, startX, "Denick Detection", Toggles.denickEnabled, v -> {
                Toggles.denickEnabled = v;
                Settings.getInstance().denickEnabled = v;
                Settings.save();
            });
            addCheckbox(tab, startX, "Bedwars Stats List (/who)", Toggles.listStatsEnabled, v -> {
                Toggles.listStatsEnabled = v;
                Settings.getInstance().listStatsEnabled = v;
                Settings.save();
            });
            addCheckbox(tab, startX, "Duels Stats", Toggles.autoStatsEnabled, v -> {
                Toggles.autoStatsEnabled = v;
                Settings.getInstance().autoStatsEnabled = v;
                Settings.save();
            });
            addCheckbox(tab, startX, "Updated Duels Titles", Toggles.duelsUpdate, v -> {
                Toggles.duelsUpdate = v;
                Settings.getInstance().duelsUpdate = v;
                Settings.save();
            });
            addCheckbox(tab, startX, "Secure Connection", !Toggles.ignoreCertificates, v -> {
                Toggles.ignoreCertificates = !v;
                Settings.getInstance().ignoreCertificates = !v;
                Settings.save();
            });
            addCheckbox(tab, startX, "Keep Original /who", Toggles.keepWhoEnabled, v -> {
                Toggles.keepWhoEnabled = v;
                Settings.getInstance().keepWhoEnabled = v;
                Settings.save();
            });
        } else if ("General".equals(tab.id)) {
            String apiKey = settings.apiKey != null ? settings.apiKey : "";
            Text apiKeyField = new Text(
                    getNextId(), startX, logicalCurrentY + labelHeightAboveComponent,
                    actualComponentWidth, "Hypixel API Key", apiKey,
                    t -> {
                        Settings.getInstance().apiKey = t;
                    },
                    focused -> {
                        if (!focused) {
                            String key = Settings.getInstance().apiKey;
                            if (key == null) {
                                key = "";
                            }
                            HypixelApiUtil.setApiKey(key);
                        }
                    }
            );
            tab.components.add(apiKeyField);
            logicalCurrentY += labelHeightAboveComponent + apiKeyField.height + interComponentSpacing;

            String skinDir = settings.skinSaveDir != null ? settings.skinSaveDir : "";
            Text skinDirField = new Text(
                    getNextId(), startX, logicalCurrentY + labelHeightAboveComponent,
                    actualComponentWidth, "Skin Save Directory (absolute path)", skinDir,
                    t -> {
                        Settings.getInstance().skinSaveDir = t != null ? t : "";
                    },
                    focused -> {
                        if (!focused) {
                            String path = Settings.getInstance().skinSaveDir;
                            if (path != null && !path.isEmpty()) {
                                File dir = new File(path);
                                if (dir.isAbsolute()) {
                                    Settings.getInstance().setSkinSaveDir(dir);
                                } else {
                                    Settings.save();
                                }
                            } else {
                                Settings.save();
                            }
                        }
                    }
            );
            tab.components.add(skinDirField);
            logicalCurrentY += labelHeightAboveComponent + skinDirField.height + interComponentSpacing;

            Slider flagIntervalSlider = new Slider(
                    getNextId(), startX, logicalCurrentY + labelHeightAboveComponent,
                    actualComponentWidth, "Anticheat Flag Interval (seconds)",
                    (float) settings.flagInterval, 0f, 20f, 0.5f,
                    v -> String.format(Locale.US, "%.1f s", v),
                    v -> Settings.getInstance().setFlagInterval(v)
            );
            tab.components.add(flagIntervalSlider);
            logicalCurrentY += labelHeightAboveComponent + flagIntervalSlider.height;
        } else if ("Warnings".equals(tab.id)) {
            Slider warnLevelSlider = new Slider(
                    getNextId(), startX, logicalCurrentY + labelHeightAboveComponent,
                    actualComponentWidth, "Warn Level (Bedwars stars, 0 = off)",
                    (float) settings.warnLevel, 0f, 3000f, 1f,
                    v -> v == 0f ? "Disabled" : String.format(Locale.US, "\u272B%.0f", v),
                    v -> {
                        Settings.getInstance().warnLevel = Math.round(v);
                        Settings.save();
                    }
            );
            tab.components.add(warnLevelSlider);
            logicalCurrentY += labelHeightAboveComponent + warnLevelSlider.height + interComponentSpacing;

            Slider warnFkdrSlider = new Slider(
                    getNextId(), startX, logicalCurrentY + labelHeightAboveComponent,
                    actualComponentWidth, "Warn FKDR (0 = off)",
                    (float) settings.warnFKDR, 0f, 50f, 0.1f,
                    v -> v == 0f ? "Disabled" : String.format(Locale.US, "%.1f FKDR", v),
                    v -> {
                        Settings.getInstance().warnFKDR = v;
                        Settings.save();
                    }
            );
            tab.components.add(warnFkdrSlider);
            logicalCurrentY += labelHeightAboveComponent + warnFkdrSlider.height;
        } else if ("AutoGG".equals(tab.id)) {
            String joined = joinAutoGGMessages(settings.autoGGMessages);
            final Text[] holder = new Text[1];
            holder[0] = new Text(
                    getNextId(), startX, logicalCurrentY + labelHeightAboveComponent,
                    actualComponentWidth, "AutoGG Messages (separate with | )", joined,
                    t -> {
                    },
                    focused -> {
                        if (!focused && holder[0] != null) {
                            saveAutoGGMessages(holder[0].getText());
                        }
                    }
            );
            tab.components.add(holder[0]);
            logicalCurrentY += labelHeightAboveComponent + holder[0].height + interComponentSpacing;

            Button saveAutoGGButton = new Button(
                    getNextId(), startX, logicalCurrentY, actualComponentWidth,
                    "Save AutoGG Messages",
                    () -> {
                        if (holder[0] != null) {
                            saveAutoGGMessages(holder[0].getText());
                        }
                    }
            );
            tab.components.add(saveAutoGGButton);
            logicalCurrentY += saveAutoGGButton.height;
        }

        tab.contentHeight = (logicalCurrentY - contentPaddingTopForComponents) + contentPaddingBottomForComponents;
        calculateContentScrollingForTab(tab);
    }

    private void addCheckbox(Tab tab, int startX, String label, boolean initial, Checkbox.OnValueChanged onChange) {
        Checkbox cb = new Checkbox(getNextId(), startX, logicalCurrentY, label, initial, onChange);
        tab.components.add(cb);
        logicalCurrentY += cb.height + interComponentSpacing;
    }

    private static String joinAutoGGMessages(String[] messages) {
        if (messages == null || messages.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(messages[i] != null ? messages[i] : "");
        }
        return sb.toString();
    }

    private static void saveAutoGGMessages(String raw) {
        if (raw == null) {
            raw = "";
        }
        String[] parts = raw.split("\\|");
        List<String> list = new ArrayList<String>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        Settings.getInstance().autoGGMessages = list.toArray(new String[0]);
        Settings.save();
    }

    private void calculateTabScrolling() {
        if (tabs.isEmpty()) {
            return;
        }
        totalTabsWidthUnscrolled = 0;
        for (int i = 0; i < tabs.size(); i++) {
            totalTabsWidthUnscrolled += tabButtonWidth + tabButtonSpacing;
        }
        totalTabsWidthUnscrolled -= tabButtonSpacing;

        int tabBarContainerWidth = panelWidth - (panelPadding * 2);
        boolean needsScrolling = totalTabsWidthUnscrolled > tabBarContainerWidth && tabs.size() > 1;
        visibleTabBarAreaWidth = needsScrolling
                ? tabBarContainerWidth - (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)
                : tabBarContainerWidth;
        maxTabScrollX = Math.max(0, totalTabsWidthUnscrolled - visibleTabBarAreaWidth);
        targetTabScrollX = Math.max(0, Math.min(maxTabScrollX, targetTabScrollX));
        tabScrollX = Math.max(0f, Math.min((float) maxTabScrollX, tabScrollX));
    }

    private void calculateContentScrollingForTab(Tab tab) {
        int tabBarYOffset = panelY + topBarHeight;
        int tabBarInternalHeight = tabBarButtonHeight + 8;
        int contentAreaMarginTop = tabBarYOffset + tabBarInternalHeight;
        int contentAreaDrawableHeight = (panelY + panelHeight - panelPadding) - contentAreaMarginTop;
        tab.maxScrollY = Math.max(0, tab.contentHeight - contentAreaDrawableHeight);
        tab.targetScrollY = Math.max(0, Math.min(tab.maxScrollY, tab.targetScrollY));
        tab.scrollY = Math.max(0f, Math.min((float) tab.maxScrollY, tab.scrollY));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Tab activeTab = currentTab();
        if (!"error_no_tabs".equals(activeTab.id)) {
            if (!isDraggingContentScrollbar) {
                float scrollYDiff = activeTab.targetScrollY - activeTab.scrollY;
                if (Math.abs(scrollYDiff) > 0.1f) {
                    activeTab.scrollY += scrollYDiff * SCROLL_SMOOTHING_FACTOR;
                } else {
                    activeTab.scrollY = activeTab.targetScrollY;
                }
            }
            float tabScrollXDiff = targetTabScrollX - tabScrollX;
            if (Math.abs(tabScrollXDiff) > 0.1f) {
                tabScrollX += tabScrollXDiff * SCROLL_SMOOTHING_FACTOR;
            } else {
                tabScrollX = targetTabScrollX;
            }
        }

        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 170).getRGB());

        drawRoundedRectUsingGL(panelX, panelY, panelWidth, panelHeight, panelCornerRadius, GuiColors.SCREEN_BACKGROUND);
        drawRoundedRectUsingGL(panelX, panelY, panelWidth, topBarHeight, panelCornerRadius, GuiColors.TITLE_BAR_BACKGROUND);
        drawRect(
                (int) (panelX + panelCornerRadius),
                (int) (panelY + topBarHeight - panelCornerRadius),
                (int) (panelX + panelWidth - panelCornerRadius),
                panelY + topBarHeight,
                GuiColors.TITLE_BAR_BACKGROUND
        );

        drawCenteredString(fontRendererObj, guiTitle, this.width / 2,
                panelY + (topBarHeight - fontRendererObj.FONT_HEIGHT) / 2, GuiColors.TITLE_BAR_TEXT);

        int closeX = panelX + panelWidth - closeButtonSize - 10;
        int closeY = panelY + (topBarHeight - closeButtonSize) / 2;
        isCloseButtonHovered = mouseX >= closeX && mouseX <= closeX + closeButtonSize
                && mouseY >= closeY && mouseY <= closeY + closeButtonSize;
        int closeColor = isCloseButtonHovered
                ? new Color(200, 50, 50, 220).getRGB()
                : new Color(80, 80, 80, 180).getRGB();
        drawRoundedRectUsingGL(closeX, closeY, closeButtonSize, closeButtonSize, 3f, closeColor);
        drawCenteredString(fontRendererObj, "\u2715", closeX + closeButtonSize / 2,
                closeY + (closeButtonSize - fontRendererObj.FONT_HEIGHT) / 2 + 1, Color.WHITE.getRGB());

        int tabBarYOffset = panelY + topBarHeight;
        int tabBarInternalHeight = tabBarButtonHeight + 8;
        drawRect(panelX, tabBarYOffset, panelX + panelWidth, tabBarYOffset + tabBarInternalHeight, GuiColors.TAB_BAR_BACKGROUND);
        Gui.drawRect(panelX, tabBarYOffset, panelX + panelWidth, tabBarYOffset + 1, GuiColors.TITLE_BAR_SEPARATOR);

        int tabsAreaX = panelX + panelPadding;
        int tabsAreaWidth = panelWidth - panelPadding * 2;
        int tabsViewportStartX = tabsAreaX;
        int localVisibleTabBarAreaWidth = tabsAreaWidth;
        boolean needsTabBarScrollButtons = totalTabsWidthUnscrolled > tabsAreaWidth;

        if (needsTabBarScrollButtons) {
            int buttonY = tabBarYOffset + (tabBarInternalHeight - tabBarButtonHeight) / 2;
            tabsViewportStartX += tabBarScrollButtonWidth + tabButtonSpacing;
            localVisibleTabBarAreaWidth -= (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2);

            int scrollLeftX = tabsAreaX;
            boolean scrollLeftHover = mouseX >= scrollLeftX && mouseX < scrollLeftX + tabBarScrollButtonWidth
                    && mouseY >= buttonY && mouseY < buttonY + tabBarButtonHeight;
            drawRoundedRectWithBorderUsingGL(scrollLeftX, buttonY, tabBarScrollButtonWidth, tabBarButtonHeight, 2f,
                    scrollLeftHover ? GuiColors.TAB_SCROLL_BUTTON_HOVER_BG : GuiColors.TAB_SCROLL_BUTTON_BG,
                    GuiColors.COMPONENT_BORDER, 1f);
            drawCenteredString(fontRendererObj, "<", scrollLeftX + tabBarScrollButtonWidth / 2,
                    buttonY + (tabBarButtonHeight - fontRendererObj.FONT_HEIGHT) / 2,
                    tabScrollX > 0f ? GuiColors.TAB_SCROLL_BUTTON_ARROW : GuiColors.TEXT_DISABLED);

            int scrollRightX = tabsAreaX + tabsAreaWidth - tabBarScrollButtonWidth;
            boolean scrollRightHover = mouseX >= scrollRightX && mouseX < scrollRightX + tabBarScrollButtonWidth
                    && mouseY >= buttonY && mouseY < buttonY + tabBarButtonHeight;
            drawRoundedRectWithBorderUsingGL(scrollRightX, buttonY, tabBarScrollButtonWidth, tabBarButtonHeight, 2f,
                    scrollRightHover ? GuiColors.TAB_SCROLL_BUTTON_HOVER_BG : GuiColors.TAB_SCROLL_BUTTON_BG,
                    GuiColors.COMPONENT_BORDER, 1f);
            drawCenteredString(fontRendererObj, ">", scrollRightX + tabBarScrollButtonWidth / 2,
                    buttonY + (tabBarButtonHeight - fontRendererObj.FONT_HEIGHT) / 2,
                    tabScrollX < maxTabScrollX ? GuiColors.TAB_SCROLL_BUTTON_ARROW : GuiColors.TEXT_DISABLED);
        }

        int tabButtonVisualY = tabBarYOffset + (tabBarInternalHeight - tabBarButtonHeight) / 2;
        startScissor(tabsViewportStartX, tabButtonVisualY, localVisibleTabBarAreaWidth, tabBarButtonHeight);
        float currentTabButtonVisualX = tabsViewportStartX - tabScrollX;
        for (int index = 0; index < tabs.size(); index++) {
            Tab tab = tabs.get(index);
            boolean isSelected = index == currentTabIndex;
            boolean tabHovered = mouseX >= currentTabButtonVisualX
                    && mouseX < currentTabButtonVisualX + tabButtonWidth
                    && mouseY >= tabButtonVisualY && mouseY < tabButtonVisualY + tabBarButtonHeight
                    && mouseX >= tabsViewportStartX
                    && mouseX < tabsViewportStartX + localVisibleTabBarAreaWidth;

            int tabBgColor;
            int textColor;
            if (isSelected) {
                tabBgColor = GuiColors.TAB_BUTTON_BACKGROUND_ACTIVE;
                textColor = GuiColors.TAB_BUTTON_TEXT_ACTIVE;
            } else if (tabHovered) {
                tabBgColor = GuiColors.TAB_BUTTON_BACKGROUND_HOVER;
                textColor = GuiColors.TAB_BUTTON_TEXT_HOVER;
            } else {
                tabBgColor = GuiColors.TAB_BUTTON_BACKGROUND_INACTIVE;
                textColor = GuiColors.TAB_BUTTON_TEXT_INACTIVE;
            }

            drawRoundedRectWithBorderUsingGL(currentTabButtonVisualX, tabButtonVisualY,
                    tabButtonWidth, tabBarButtonHeight, 3f, tabBgColor, GuiColors.TAB_BAR_BORDER, 1f);
            if (isSelected) {
                Gui.drawRect((int) currentTabButtonVisualX + 3, tabButtonVisualY + tabBarButtonHeight - 2,
                        (int) currentTabButtonVisualX + tabButtonWidth - 3,
                        tabButtonVisualY + tabBarButtonHeight - 1, GuiColors.PRIMARY_BLUE_BRIGHT);
            }
            drawCenteredString(fontRendererObj, tab.name,
                    (int) currentTabButtonVisualX + tabButtonWidth / 2,
                    tabButtonVisualY + (tabBarButtonHeight - fontRendererObj.FONT_HEIGHT) / 2, textColor);
            currentTabButtonVisualX += tabButtonWidth + tabButtonSpacing;
        }
        stopScissor();

        int contentAreaVisualTop = tabBarYOffset + tabBarInternalHeight;
        int contentAreaVisualBottom = panelY + panelHeight - panelPadding;
        int contentAreaDrawableHeight = contentAreaVisualBottom - contentAreaVisualTop;

        Gui.drawRect(panelX, contentAreaVisualTop, panelX + panelWidth, contentAreaVisualTop + 1, GuiColors.TITLE_BAR_SEPARATOR);
        drawRoundedRectWithBorderUsingGL(
                panelX + panelPadding, contentAreaVisualTop + panelPadding,
                panelWidth - panelPadding * 2, contentAreaDrawableHeight - panelPadding * 2,
                3f, GuiColors.MODERN_SECONDARY_BACKGROUND, GuiColors.COMPONENT_BORDER, 1f
        );

        int contentAreaX = panelX + panelPadding + 1;
        int contentAreaY = contentAreaVisualTop + panelPadding + 1;
        int contentAreaHeight = contentAreaDrawableHeight - panelPadding * 2 - 2;
        int contentAreaWidth = panelWidth - panelPadding * 2 - 2;
        if (activeTab.maxScrollY > 0) {
            contentAreaWidth -= (scrollbarWidth + scrollbarMargin);
        }

        startScissor(contentAreaX, contentAreaY, contentAreaWidth, contentAreaHeight);

        if (!"error_no_tabs".equals(activeTab.id)) {
            for (GuiComponentBase component : activeTab.components) {
                int originalLogicalY = component.y;
                int componentScreenY = contentAreaY + originalLogicalY - (int) activeTab.scrollY;
                if (componentScreenY + component.height >= contentAreaY
                        && componentScreenY <= contentAreaY + contentAreaHeight) {
                    component.y = componentScreenY;
                    if (!(component instanceof Dropdown && ((Dropdown) component).isOpen)) {
                        component.drawComponent(mouseX, mouseY, partialTicks);
                    }
                    component.y = originalLogicalY;
                }
            }
        }
        stopScissor();

        if (activeTab.maxScrollY > 0) {
            int scrollBarActualX = contentAreaX + contentAreaWidth + scrollbarMargin;
            int scrollBarTrackY = contentAreaY;
            int scrollBarTrackHeight = contentAreaHeight;
            drawRoundedRectUsingGL(scrollBarActualX, scrollBarTrackY, scrollbarWidth, scrollBarTrackHeight, 3f, GuiColors.SCROLLBAR_BG);

            float thumbHeightRatio = Math.max(0.05f, Math.min(1f, contentAreaHeight / (float) activeTab.contentHeight));
            int thumbHeight = Math.max(20, (int) (scrollBarTrackHeight * thumbHeightRatio));
            float thumbYRatio = activeTab.maxScrollY > 0 ? activeTab.scrollY / activeTab.maxScrollY : 0f;
            int thumbYPos = scrollBarTrackY + (int) ((scrollBarTrackHeight - thumbHeight) * thumbYRatio);
            boolean thumbHovered = (mouseX >= scrollBarActualX && mouseX < scrollBarActualX + scrollbarWidth
                    && mouseY >= thumbYPos && mouseY < thumbYPos + thumbHeight) || isDraggingContentScrollbar;

            float thumbYClamped = Math.max(scrollBarTrackY,
                    Math.min(scrollBarTrackY + scrollBarTrackHeight - thumbHeight, thumbYPos));
            drawRoundedRectUsingGL(scrollBarActualX + 1f, thumbYClamped, scrollbarWidth - 2f, thumbHeight, 3f,
                    thumbHovered ? GuiColors.MODERN_SCROLLBAR_THUMB_HOVER : GuiColors.SCROLLBAR_THUMB);
        }

        if (openDropdown != null) {
            Dropdown dd = openDropdown;
            int originalLogicalYDd = dd.y;
            dd.y = contentAreaY + originalLogicalYDd - (int) activeTab.scrollY;
            dd.drawComponent(mouseX, mouseY, partialTicks);
            dd.y = originalLogicalYDd;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }

        int closeX = panelX + panelWidth - closeButtonSize - 10;
        int closeY = panelY + (topBarHeight - closeButtonSize) / 2;
        if (mouseX >= closeX && mouseX <= closeX + closeButtonSize
                && mouseY >= closeY && mouseY <= closeY + closeButtonSize) {
            this.mc.displayGuiScreen(null);
            return;
        }

        int tabBarYOffset = panelY + topBarHeight;
        int tabBarInternalHeight = tabBarButtonHeight + 8;
        int tabsAreaX = panelX + panelPadding;
        int tabsAreaWidth = panelWidth - panelPadding * 2;
        int tabsViewportStartX = tabsAreaX;
        boolean needsTabBarScrollButtons = totalTabsWidthUnscrolled > tabsAreaWidth;

        if (needsTabBarScrollButtons) {
            int buttonY = tabBarYOffset + (tabBarInternalHeight - tabBarButtonHeight) / 2;
            int scrollLeftX = tabsAreaX;
            if (mouseX >= scrollLeftX && mouseX < scrollLeftX + tabBarScrollButtonWidth
                    && mouseY >= buttonY && mouseY < buttonY + tabBarButtonHeight) {
                targetTabScrollX = Math.max(0, targetTabScrollX - (tabButtonWidth + tabButtonSpacing));
                return;
            }
            int scrollRightX = tabsAreaX + tabsAreaWidth - tabBarScrollButtonWidth;
            if (mouseX >= scrollRightX && mouseX < scrollRightX + tabBarScrollButtonWidth
                    && mouseY >= buttonY && mouseY < buttonY + tabBarButtonHeight) {
                targetTabScrollX = Math.min(maxTabScrollX, targetTabScrollX + (tabButtonWidth + tabButtonSpacing));
                return;
            }
            tabsViewportStartX += tabBarScrollButtonWidth + tabButtonSpacing;
        }

        int tabButtonVisualY = tabBarYOffset + (tabBarInternalHeight - tabBarButtonHeight) / 2;
        int actualClickableTabBarWidth = needsTabBarScrollButtons
                ? tabsAreaWidth - (tabBarScrollButtonWidth * 2 + tabButtonSpacing * 2)
                : tabsAreaWidth;
        int currentTabButtonVisualX = tabsViewportStartX - (int) tabScrollX;
        for (int index = 0; index < tabs.size(); index++) {
            if (mouseX >= currentTabButtonVisualX && mouseX < currentTabButtonVisualX + tabButtonWidth
                    && mouseY >= tabButtonVisualY && mouseY < tabButtonVisualY + tabBarButtonHeight
                    && mouseX >= tabsViewportStartX
                    && mouseX < tabsViewportStartX + actualClickableTabBarWidth) {
                if (currentTabIndex != index) {
                    for (GuiComponentBase c : currentTab().components) {
                        if (c instanceof Dropdown) {
                            ((Dropdown) c).close();
                        }
                        if (c instanceof Text) {
                            ((Text) c).setFocused(false);
                        }
                    }
                    openDropdown = null;
                    isDraggingContentScrollbar = false;
                    currentTabIndex = index;
                    currentTab().targetScrollY = 0;
                    currentTab().scrollY = 0f;
                }
                return;
            }
            currentTabButtonVisualX += tabButtonWidth + tabButtonSpacing;
        }

        Tab activeTab = currentTab();
        if ("error_no_tabs".equals(activeTab.id)) {
            return;
        }

        int contentAreaVisualTop = tabBarYOffset + tabBarInternalHeight;
        int contentAreaX = panelX + panelPadding + 1;
        int contentAreaY = contentAreaVisualTop + panelPadding + 1;
        int contentAreaWidth = panelWidth - panelPadding * 2 - 2;

        if (this.openDropdown != null) {
            Dropdown dd = this.openDropdown;
            int oY = dd.y;
            dd.y = contentAreaY + oY - (int) activeTab.scrollY;
            if (dd.mouseClicked(mouseX, mouseY, mouseButton)) {
                dd.y = oY;
                if (!dd.isOpen) {
                    this.openDropdown = null;
                }
                return;
            }
            dd.y = oY;
            int listH = dd.isOpen
                    ? Math.min(dd.getOptions().size(), dd.maxDisplayableOptions) * dd.optionHeight
                    : 0;
            boolean clickInside = mouseX >= dd.x && mouseX < dd.x + dd.width
                    && mouseY >= dd.y && mouseY < dd.y + dd.height + listH;
            if (!clickInside) {
                dd.close();
                this.openDropdown = null;
            } else {
                return;
            }
        }

        boolean clickedComponent = false;
        if (mouseX > contentAreaX && mouseX < contentAreaX + contentAreaWidth) {
            for (int i = activeTab.components.size() - 1; i >= 0; i--) {
                GuiComponentBase component = activeTab.components.get(i);
                int oY = component.y;
                component.y = contentAreaY + oY - (int) activeTab.scrollY;
                if (component.mouseClicked(mouseX, mouseY, mouseButton)) {
                    if (component instanceof Dropdown) {
                        if (((Dropdown) component).isOpen) {
                            this.openDropdown = (Dropdown) component;
                        }
                    } else if (component instanceof Text) {
                        for (GuiComponentBase other : activeTab.components) {
                            if (other instanceof Text && other != component) {
                                ((Text) other).setFocused(false);
                            }
                        }
                    }
                    clickedComponent = true;
                }
                component.y = oY;
                if (clickedComponent) {
                    break;
                }
            }
        }

        if (activeTab.maxScrollY > 0 && mouseX >= contentAreaX + contentAreaWidth + scrollbarMargin) {
            isDraggingContentScrollbar = true;
            contentScrollbarMouseDragStartY = mouseY;
            contentScrollbarInitialScrollY = activeTab.scrollY;
        }

        if (!clickedComponent) {
            for (GuiComponentBase c : activeTab.components) {
                if (c instanceof Text) {
                    ((Text) c).setFocused(false);
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            isDraggingContentScrollbar = false;
        }
        try {
            super.mouseReleased(mouseX, mouseY, state);
        } catch (Exception ignored) {
        }
        Tab activeTab = currentTab();
        int contentAreaY = panelY + topBarHeight + (tabBarButtonHeight + 8) + panelPadding + 1;
        for (GuiComponentBase it : activeTab.components) {
            int oY = it.y;
            it.y = contentAreaY + oY - (int) activeTab.scrollY;
            it.mouseReleased(mouseX, mouseY, state);
            it.y = oY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingContentScrollbar) {
            Tab activeTab = currentTab();
            int contentAreaY = panelY + topBarHeight + (tabBarButtonHeight + 8) + panelPadding + 1;
            int contentAreaH = (panelY + panelHeight - panelPadding) - contentAreaY;
            int thumbH = Math.max(20, (int) (contentAreaH / (float) Math.max(1, activeTab.contentHeight) * contentAreaH));
            int scrollablePixelRange = contentAreaH - thumbH;

            if (scrollablePixelRange > 0 && activeTab.maxScrollY > 0) {
                float deltaY = mouseY - contentScrollbarMouseDragStartY;
                float scrollChange = deltaY * (activeTab.maxScrollY / (float) scrollablePixelRange);
                activeTab.scrollY = Math.max(0f, Math.min((float) activeTab.maxScrollY,
                        contentScrollbarInitialScrollY + scrollChange));
                activeTab.targetScrollY = Math.round(activeTab.scrollY);
            }
            return;
        }

        try {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        } catch (Exception ignored) {
        }
        Tab activeTab = currentTab();
        int contentAreaY = panelY + topBarHeight + (tabBarButtonHeight + 8) + panelPadding + 1;
        for (GuiComponentBase c : activeTab.components) {
            if (c instanceof Slider) {
                int oY = c.y;
                c.y = contentAreaY + oY - (int) activeTab.scrollY;
                c.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                c.y = oY;
            }
        }
    }

    @Override
    public void handleMouseInput() {
        try {
            super.handleMouseInput();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int rawMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int rawMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            if (rawMouseX > panelX && rawMouseX < panelX + panelWidth
                    && rawMouseY > panelY && rawMouseY < panelY + panelHeight) {
                Tab activeTab = currentTab();
                int scrollAmount = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 90 : 45;
                activeTab.targetScrollY = Math.max(0, Math.min(activeTab.maxScrollY,
                        activeTab.targetScrollY - dWheel / 120 * scrollAmount));
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE
                || keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
            if (openDropdown != null) {
                openDropdown.close();
                openDropdown = null;
                return;
            }
            this.mc.displayGuiScreen(null);
            return;
        }
        for (GuiComponentBase c : currentTab().components) {
            if (c.keyTyped(typedChar, keyCode)) {
                return;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        // Persist any focused textfields by unfocusing (triggers blur save)
        for (Tab tab : tabs) {
            for (GuiComponentBase c : tab.components) {
                if (c instanceof Text) {
                    ((Text) c).unfocusIfNeeded();
                }
            }
        }
        Settings.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void startScissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        if (width <= 0 || height <= 0) {
            return;
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - (y + height)) * scale, width * scale, height * scale);
    }

    private void stopScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawRoundedRectUsingGL(float x, float y, float width, float height, float radius, int colorInt) {
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

    private void drawRoundedRectWithBorderUsingGL(float x, float y, float width, float height,
                                                   float radius, int bgColor, int borderColor, float borderWidth) {
        drawRoundedRectUsingGL(x, y, width, height, radius, borderColor);
        drawRoundedRectUsingGL(
                x + borderWidth, y + borderWidth,
                width - borderWidth * 2, height - borderWidth * 2,
                Math.max(0f, radius - borderWidth), bgColor
        );
    }
}
