package com.konayuki.statflex.gui;

import java.awt.Color;

/**
 * Color palette for the statflex config GUI (ported from WLR GuiColors).
 */

public final class GuiColors {

    private GuiColors() {
    }

    public static final int PRIMARY_BLUE = new Color(40, 120, 200).getRGB();
    public static final int PRIMARY_BLUE_BRIGHT = new Color(80, 160, 255).getRGB();
    public static final int PRIMARY_BLUE_DARK = new Color(20, 90, 150).getRGB();
    public static final int LIGHT_BLUE = new Color(100, 180, 255).getRGB();

    public static final int SCREEN_BACKGROUND = new Color(15, 15, 18).getRGB();
    public static final int COMPONENT_BACKGROUND = new Color(25, 25, 28).getRGB();
    public static final int COMPONENT_BACKGROUND_HOVER = new Color(35, 35, 40).getRGB();
    public static final int COMPONENT_BACKGROUND_DISABLED = new Color(20, 20, 23).getRGB();

    public static final int COMPONENT_BORDER = new Color(40, 40, 45).getRGB();
    public static final int COMPONENT_BORDER_BLUE = PRIMARY_BLUE_DARK;
    public static final int COMPONENT_BORDER_FOCUSED_BLUE = PRIMARY_BLUE_BRIGHT;

    public static final int TEXT_PRIMARY = new Color(220, 220, 225).getRGB();
    public static final int TEXT_SECONDARY = new Color(180, 180, 180).getRGB();
    public static final int TEXT_ACCENT = PRIMARY_BLUE_BRIGHT;
    public static final int TEXT_DISABLED = new Color(100, 100, 105).getRGB();
    public static final int TEXT_ON_BLUE_BACKGROUND = new Color(240, 240, 240).getRGB();

    public static final int TRANSPARENT_BLACK_VERY_LIGHT = new Color(0, 0, 0, 20).getRGB();
    public static final int TRANSPARENT_BLACK_LIGHT = new Color(0, 0, 0, 45).getRGB();
    public static final int TRANSPARENT_BLACK_MEDIUM = new Color(0, 0, 0, 75).getRGB();

    private static final Color TEXT_PRIMARY_AWT = new Color(TEXT_PRIMARY, true);
    public static final int TRANSPARENT_TEXT_PRIMARY_VERY_LIGHT =
            new Color(TEXT_PRIMARY_AWT.getRed(), TEXT_PRIMARY_AWT.getGreen(), TEXT_PRIMARY_AWT.getBlue(), 20).getRGB();
    public static final int TRANSPARENT_TEXT_PRIMARY_LIGHT =
            new Color(TEXT_PRIMARY_AWT.getRed(), TEXT_PRIMARY_AWT.getGreen(), TEXT_PRIMARY_AWT.getBlue(), 35).getRGB();

    private static final Color PRIMARY_BLUE_BRIGHT_AWT_FOR_HIGHLIGHT = new Color(PRIMARY_BLUE_BRIGHT, true);
    public static final int TRANSPARENT_BLUE_VERY_LIGHT_HIGHLIGHT = new Color(
            PRIMARY_BLUE_BRIGHT_AWT_FOR_HIGHLIGHT.getRed(),
            PRIMARY_BLUE_BRIGHT_AWT_FOR_HIGHLIGHT.getGreen(),
            PRIMARY_BLUE_BRIGHT_AWT_FOR_HIGHLIGHT.getBlue(),
            30
    ).getRGB();

    public static final int TAB_BAR_BACKGROUND = new Color(20, 20, 22).getRGB();
    public static final int TAB_BUTTON_BACKGROUND_INACTIVE = TAB_BAR_BACKGROUND;
    public static final int TAB_BUTTON_BACKGROUND_HOVER = COMPONENT_BACKGROUND_HOVER;
    public static final int TAB_BUTTON_BACKGROUND_ACTIVE = PRIMARY_BLUE;
    public static final int TAB_BUTTON_TEXT_INACTIVE = new Color(160, 160, 165).getRGB();
    public static final int TAB_BUTTON_TEXT_HOVER = TEXT_PRIMARY;
    public static final int TAB_BUTTON_TEXT_ACTIVE = TEXT_ON_BLUE_BACKGROUND;
    public static final int TAB_BAR_BORDER = new Color(30, 30, 33).getRGB();
    public static final int TAB_SCROLL_BUTTON_BG = new Color(35, 35, 40).getRGB();
    public static final int TAB_SCROLL_BUTTON_HOVER_BG = new Color(45, 45, 50).getRGB();
    public static final int TAB_SCROLL_BUTTON_ARROW = TEXT_PRIMARY;

    public static final int CATEGORY_TITLE_TEXT = TEXT_ON_BLUE_BACKGROUND;
    private static final Color PRIMARY_BLUE_DARK_AWT_FOR_CAT_BG = new Color(PRIMARY_BLUE_DARK, true);
    public static final int CATEGORY_TITLE_BACKGROUND = new Color(
            PRIMARY_BLUE_DARK_AWT_FOR_CAT_BG.getRed(),
            PRIMARY_BLUE_DARK_AWT_FOR_CAT_BG.getGreen(),
            PRIMARY_BLUE_DARK_AWT_FOR_CAT_BG.getBlue(),
            200
    ).getRGB();
    private static final Color PRIMARY_BLUE_AWT_FOR_CAT_BORDER = new Color(PRIMARY_BLUE, true);
    public static final int CATEGORY_TITLE_BORDER = new Color(
            PRIMARY_BLUE_AWT_FOR_CAT_BORDER.getRed(),
            PRIMARY_BLUE_AWT_FOR_CAT_BORDER.getGreen(),
            PRIMARY_BLUE_AWT_FOR_CAT_BORDER.getBlue(),
            180
    ).getRGB();
    public static final int CATEGORY_SEPARATOR_LINE = new Color(
            PRIMARY_BLUE_DARK_AWT_FOR_CAT_BG.getRed(),
            PRIMARY_BLUE_DARK_AWT_FOR_CAT_BG.getGreen(),
            PRIMARY_BLUE_DARK_AWT_FOR_CAT_BG.getBlue(),
            150
    ).getRGB();

    public static final int SCROLLBAR_BG = new Color(20, 20, 22).getRGB();
    public static final int SCROLLBAR_THUMB = PRIMARY_BLUE;
    public static final int MODERN_SCROLLBAR_THUMB_HOVER = PRIMARY_BLUE_BRIGHT;

    public static final int CHECKBOX_BOX = COMPONENT_BACKGROUND;
    public static final int CHECKBOX_BOX_HOVER = COMPONENT_BACKGROUND_HOVER;
    public static final int CHECKBOX_CHECK = PRIMARY_BLUE_BRIGHT;

    public static final int DROPDOWN_ARROW = TEXT_PRIMARY;
    public static final int DROPDOWN_BACKGROUND_OPEN = new Color(20, 20, 22, 250).getRGB();
    public static final int DROPDOWN_ITEM_TEXT = TEXT_PRIMARY;
    private static final Color P_BLUE_AWT_FOR_DROPDOWN = new Color(PRIMARY_BLUE, true);
    public static final int DROPDOWN_ITEM_HOVER_BG = new Color(
            P_BLUE_AWT_FOR_DROPDOWN.getRed(),
            P_BLUE_AWT_FOR_DROPDOWN.getGreen(),
            P_BLUE_AWT_FOR_DROPDOWN.getBlue(),
            90
    ).getRGB();
    public static final int DROPDOWN_ITEM_SELECTED_BG = new Color(
            P_BLUE_AWT_FOR_DROPDOWN.getRed(),
            P_BLUE_AWT_FOR_DROPDOWN.getGreen(),
            P_BLUE_AWT_FOR_DROPDOWN.getBlue(),
            130
    ).getRGB();

    public static final int SLIDER_TRACK = new Color(35, 35, 40).getRGB();
    public static final int SLIDER_TRACK_FILLED = PRIMARY_BLUE;
    public static final int SLIDER_KNOB_BLUE_THEME = PRIMARY_BLUE;
    public static final int SLIDER_KNOB_HOVER_BLUE_THEME = PRIMARY_BLUE_BRIGHT;
    public static final int SLIDER_KNOB = SLIDER_KNOB_BLUE_THEME;
    public static final int SLIDER_KNOB_HOVER = SLIDER_KNOB_HOVER_BLUE_THEME;

    public static final int TEXTFIELD_BACKGROUND = new Color(20, 20, 22).getRGB();
    public static final int TEXTFIELD_BORDER = COMPONENT_BORDER;
    public static final int TEXTFIELD_BORDER_FOCUSED = PRIMARY_BLUE_BRIGHT;
    public static final int TEXTFIELD_TEXT = TEXT_PRIMARY;

    public static final int MODERN_PRIMARY_BACKGROUND = SCREEN_BACKGROUND;
    public static final int MODERN_SECONDARY_BACKGROUND = COMPONENT_BACKGROUND;

    public static final int MODERN_ACCENT_PRIMARY = PRIMARY_BLUE_BRIGHT;
    public static final int MODERN_ACCENT_SECONDARY = PRIMARY_BLUE;

    public static final int MODERN_UI_ELEMENT_BORDER = COMPONENT_BORDER;
    public static final int MODERN_DIVIDER_COLOR = new Color(30, 30, 33).getRGB();

    public static final int MODERN_COMPONENT_HOVER = COMPONENT_BACKGROUND_HOVER;
    public static final int MODERN_COMPONENT_ACTIVE = LIGHT_BLUE;
    public static final int MODERN_COMPONENT_DISABLED_BG = COMPONENT_BACKGROUND_DISABLED;

    public static final int BUTTON_MODERN_BACKGROUND = PRIMARY_BLUE;
    public static final int BUTTON_MODERN_BACKGROUND_HOVER = PRIMARY_BLUE_BRIGHT;
    public static final int BUTTON_MODERN_TEXT = TEXT_ON_BLUE_BACKGROUND;

    public static final int TITLE_BAR_BACKGROUND = new Color(18, 18, 20).getRGB();
    public static final int TITLE_BAR_TEXT = TEXT_PRIMARY;
    public static final int TITLE_BAR_SEPARATOR = PRIMARY_BLUE_DARK;

    private static final Color P_BLUE_BRIGHT_AWT_FOR_GLOW = new Color(PRIMARY_BLUE_BRIGHT, true);
    public static final int PRIMARY_BLUE_BRIGHT_GLOW_EFFECT = new Color(
            P_BLUE_BRIGHT_AWT_FOR_GLOW.getRed(),
            P_BLUE_BRIGHT_AWT_FOR_GLOW.getGreen(),
            P_BLUE_BRIGHT_AWT_FOR_GLOW.getBlue(),
            80
    ).getRGB();

    private static final Color SCREEN_BG_AWT_FOR_SHADOW = new Color(SCREEN_BACKGROUND, true);
    public static final int SUBTLE_SHADOW_COLOR = new Color(
            Math.max(0, SCREEN_BG_AWT_FOR_SHADOW.getRed() - 3),
            Math.max(0, SCREEN_BG_AWT_FOR_SHADOW.getGreen() - 3),
            Math.max(0, SCREEN_BG_AWT_FOR_SHADOW.getBlue() - 3),
            120
    ).getRGB();
}
