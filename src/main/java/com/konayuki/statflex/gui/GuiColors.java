package com.konayuki.statflex.gui;

import com.konayuki.statflex.utils.Settings;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GuiColors {

    private GuiColors() {
    }

    public static final int DEFAULT_PRIMARY_BLUE = new Color(40, 120, 200).getRGB();
    public static final int DEFAULT_PRIMARY_BLUE_BRIGHT = new Color(80, 160, 255).getRGB();
    public static final int DEFAULT_PRIMARY_BLUE_DARK = new Color(20, 90, 150).getRGB();
    public static final int DEFAULT_LIGHT_BLUE = new Color(100, 180, 255).getRGB();
    public static final int DEFAULT_SCREEN_BACKGROUND = new Color(15, 15, 18).getRGB();
    public static final int DEFAULT_COMPONENT_BACKGROUND = new Color(25, 25, 28).getRGB();
    public static final int DEFAULT_COMPONENT_BACKGROUND_HOVER = new Color(35, 35, 40).getRGB();
    public static final int DEFAULT_COMPONENT_BACKGROUND_DISABLED = new Color(20, 20, 23).getRGB();
    public static final int DEFAULT_COMPONENT_BORDER = new Color(40, 40, 45).getRGB();
    public static final int DEFAULT_COMPONENT_BORDER_BLUE = DEFAULT_PRIMARY_BLUE_DARK;
    public static final int DEFAULT_COMPONENT_BORDER_FOCUSED_BLUE = DEFAULT_PRIMARY_BLUE_BRIGHT;
    public static final int DEFAULT_TEXT_PRIMARY = new Color(220, 220, 225).getRGB();
    public static final int DEFAULT_TEXT_SECONDARY = new Color(180, 180, 180).getRGB();
    public static final int DEFAULT_TEXT_ACCENT = DEFAULT_PRIMARY_BLUE_BRIGHT;
    public static final int DEFAULT_TEXT_DISABLED = new Color(100, 100, 105).getRGB();
    public static final int DEFAULT_TEXT_ON_BLUE_BACKGROUND = new Color(240, 240, 240).getRGB();

    public static final String[] SYSTEM_COLOR_KEYS = {
            "PRIMARY_BLUE",
            "PRIMARY_BLUE_BRIGHT",
            "PRIMARY_BLUE_DARK",
            "LIGHT_BLUE",
            "SCREEN_BACKGROUND",
            "COMPONENT_BACKGROUND",
            "COMPONENT_BACKGROUND_HOVER",
            "COMPONENT_BACKGROUND_DISABLED",
            "COMPONENT_BORDER",
            "COMPONENT_BORDER_BLUE",
            "COMPONENT_BORDER_FOCUSED_BLUE",
            "TEXT_PRIMARY",
            "TEXT_SECONDARY",
            "TEXT_ACCENT",
            "TEXT_DISABLED",
            "TEXT_ON_BLUE_BACKGROUND"
    };

    public static final String[] SYSTEM_COLOR_LABELS = {
            "Primary Blue",
            "Primary Blue Bright",
            "Primary Blue Dark",
            "Light Blue",
            "Screen Background",
            "Component Background",
            "Component Background Hover",
            "Component Background Disabled",
            "Component Border",
            "Component Border Blue",
            "Component Border Focused",
            "Text Primary",
            "Text Secondary",
            "Text Accent",
            "Text Disabled",
            "Text On Blue"
    };

    public static int PRIMARY_BLUE = DEFAULT_PRIMARY_BLUE;
    public static int PRIMARY_BLUE_BRIGHT = DEFAULT_PRIMARY_BLUE_BRIGHT;
    public static int PRIMARY_BLUE_DARK = DEFAULT_PRIMARY_BLUE_DARK;
    public static int LIGHT_BLUE = DEFAULT_LIGHT_BLUE;

    public static int SCREEN_BACKGROUND = DEFAULT_SCREEN_BACKGROUND;
    public static int COMPONENT_BACKGROUND = DEFAULT_COMPONENT_BACKGROUND;
    public static int COMPONENT_BACKGROUND_HOVER = DEFAULT_COMPONENT_BACKGROUND_HOVER;
    public static int COMPONENT_BACKGROUND_DISABLED = DEFAULT_COMPONENT_BACKGROUND_DISABLED;

    public static int COMPONENT_BORDER = DEFAULT_COMPONENT_BORDER;
    public static int COMPONENT_BORDER_BLUE = DEFAULT_COMPONENT_BORDER_BLUE;
    public static int COMPONENT_BORDER_FOCUSED_BLUE = DEFAULT_COMPONENT_BORDER_FOCUSED_BLUE;

    public static int TEXT_PRIMARY = DEFAULT_TEXT_PRIMARY;
    public static int TEXT_SECONDARY = DEFAULT_TEXT_SECONDARY;
    public static int TEXT_ACCENT = DEFAULT_TEXT_ACCENT;
    public static int TEXT_DISABLED = DEFAULT_TEXT_DISABLED;
    public static int TEXT_ON_BLUE_BACKGROUND = DEFAULT_TEXT_ON_BLUE_BACKGROUND;

    public static int TRANSPARENT_BLACK_VERY_LIGHT = new Color(0, 0, 0, 20).getRGB();
    public static int TRANSPARENT_BLACK_LIGHT = new Color(0, 0, 0, 45).getRGB();
    public static int TRANSPARENT_BLACK_MEDIUM = new Color(0, 0, 0, 75).getRGB();

    public static int TRANSPARENT_TEXT_PRIMARY_VERY_LIGHT;
    public static int TRANSPARENT_TEXT_PRIMARY_LIGHT;
    public static int TRANSPARENT_BLUE_VERY_LIGHT_HIGHLIGHT;

    public static int TAB_BAR_BACKGROUND = new Color(20, 20, 22).getRGB();
    public static int TAB_BUTTON_BACKGROUND_INACTIVE;
    public static int TAB_BUTTON_BACKGROUND_HOVER;
    public static int TAB_BUTTON_BACKGROUND_ACTIVE;
    public static int TAB_BUTTON_TEXT_INACTIVE = new Color(160, 160, 165).getRGB();
    public static int TAB_BUTTON_TEXT_HOVER;
    public static int TAB_BUTTON_TEXT_ACTIVE;
    public static int TAB_BAR_BORDER = new Color(30, 30, 33).getRGB();
    public static int TAB_SCROLL_BUTTON_BG = new Color(35, 35, 40).getRGB();
    public static int TAB_SCROLL_BUTTON_HOVER_BG = new Color(45, 45, 50).getRGB();
    public static int TAB_SCROLL_BUTTON_ARROW;

    public static int CATEGORY_TITLE_TEXT;
    public static int CATEGORY_TITLE_BACKGROUND;
    public static int CATEGORY_TITLE_BORDER;
    public static int CATEGORY_SEPARATOR_LINE;

    public static int SCROLLBAR_BG = new Color(20, 20, 22).getRGB();
    public static int SCROLLBAR_THUMB;
    public static int MODERN_SCROLLBAR_THUMB_HOVER;

    public static int CHECKBOX_BOX;
    public static int CHECKBOX_BOX_HOVER;
    public static int CHECKBOX_CHECK;

    public static int DROPDOWN_ARROW;
    public static int DROPDOWN_BACKGROUND_OPEN = new Color(20, 20, 22, 250).getRGB();
    public static int DROPDOWN_ITEM_TEXT;
    public static int DROPDOWN_ITEM_HOVER_BG;
    public static int DROPDOWN_ITEM_SELECTED_BG;

    public static int SLIDER_TRACK = new Color(35, 35, 40).getRGB();
    public static int SLIDER_TRACK_FILLED;
    public static int SLIDER_KNOB_BLUE_THEME;
    public static int SLIDER_KNOB_HOVER_BLUE_THEME;
    public static int SLIDER_KNOB;
    public static int SLIDER_KNOB_HOVER;

    public static int TEXTFIELD_BACKGROUND = new Color(20, 20, 22).getRGB();
    public static int TEXTFIELD_BORDER;
    public static int TEXTFIELD_BORDER_FOCUSED;
    public static int TEXTFIELD_TEXT;

    public static int MODERN_PRIMARY_BACKGROUND;
    public static int MODERN_SECONDARY_BACKGROUND;
    public static int MODERN_ACCENT_PRIMARY;
    public static int MODERN_ACCENT_SECONDARY;
    public static int MODERN_UI_ELEMENT_BORDER;
    public static int MODERN_DIVIDER_COLOR = new Color(30, 30, 33).getRGB();
    public static int MODERN_COMPONENT_HOVER;
    public static int MODERN_COMPONENT_ACTIVE;
    public static int MODERN_COMPONENT_DISABLED_BG;

    public static int BUTTON_MODERN_BACKGROUND;
    public static int BUTTON_MODERN_BACKGROUND_HOVER;
    public static int BUTTON_MODERN_TEXT;

    public static int TITLE_BAR_BACKGROUND = new Color(18, 18, 20).getRGB();
    public static int TITLE_BAR_TEXT;
    public static int TITLE_BAR_SEPARATOR;

    public static int PRIMARY_BLUE_BRIGHT_GLOW_EFFECT;
    public static int SUBTLE_SHADOW_COLOR;

    static {
        refreshDerived();
    }

    public static void applyDefaults() {
        PRIMARY_BLUE = DEFAULT_PRIMARY_BLUE;
        PRIMARY_BLUE_BRIGHT = DEFAULT_PRIMARY_BLUE_BRIGHT;
        PRIMARY_BLUE_DARK = DEFAULT_PRIMARY_BLUE_DARK;
        LIGHT_BLUE = DEFAULT_LIGHT_BLUE;
        SCREEN_BACKGROUND = DEFAULT_SCREEN_BACKGROUND;
        COMPONENT_BACKGROUND = DEFAULT_COMPONENT_BACKGROUND;
        COMPONENT_BACKGROUND_HOVER = DEFAULT_COMPONENT_BACKGROUND_HOVER;
        COMPONENT_BACKGROUND_DISABLED = DEFAULT_COMPONENT_BACKGROUND_DISABLED;
        COMPONENT_BORDER = DEFAULT_COMPONENT_BORDER;
        COMPONENT_BORDER_BLUE = DEFAULT_COMPONENT_BORDER_BLUE;
        COMPONENT_BORDER_FOCUSED_BLUE = DEFAULT_COMPONENT_BORDER_FOCUSED_BLUE;
        TEXT_PRIMARY = DEFAULT_TEXT_PRIMARY;
        TEXT_SECONDARY = DEFAULT_TEXT_SECONDARY;
        TEXT_ACCENT = DEFAULT_TEXT_ACCENT;
        TEXT_DISABLED = DEFAULT_TEXT_DISABLED;
        TEXT_ON_BLUE_BACKGROUND = DEFAULT_TEXT_ON_BLUE_BACKGROUND;
        refreshDerived();
    }

    public static void refreshDerived() {
        Color textPrimaryAwt = new Color(TEXT_PRIMARY, true);
        TRANSPARENT_TEXT_PRIMARY_VERY_LIGHT = new Color(
                textPrimaryAwt.getRed(), textPrimaryAwt.getGreen(), textPrimaryAwt.getBlue(), 20).getRGB();
        TRANSPARENT_TEXT_PRIMARY_LIGHT = new Color(
                textPrimaryAwt.getRed(), textPrimaryAwt.getGreen(), textPrimaryAwt.getBlue(), 35).getRGB();

        Color brightAwt = new Color(PRIMARY_BLUE_BRIGHT, true);
        TRANSPARENT_BLUE_VERY_LIGHT_HIGHLIGHT = new Color(
                brightAwt.getRed(), brightAwt.getGreen(), brightAwt.getBlue(), 30).getRGB();
        PRIMARY_BLUE_BRIGHT_GLOW_EFFECT = new Color(
                brightAwt.getRed(), brightAwt.getGreen(), brightAwt.getBlue(), 80).getRGB();

        TAB_BUTTON_BACKGROUND_INACTIVE = TAB_BAR_BACKGROUND;
        TAB_BUTTON_BACKGROUND_HOVER = COMPONENT_BACKGROUND_HOVER;
        TAB_BUTTON_BACKGROUND_ACTIVE = PRIMARY_BLUE;
        TAB_BUTTON_TEXT_HOVER = TEXT_PRIMARY;
        TAB_BUTTON_TEXT_ACTIVE = TEXT_ON_BLUE_BACKGROUND;
        TAB_SCROLL_BUTTON_ARROW = TEXT_PRIMARY;

        CATEGORY_TITLE_TEXT = TEXT_ON_BLUE_BACKGROUND;
        Color darkAwt = new Color(PRIMARY_BLUE_DARK, true);
        CATEGORY_TITLE_BACKGROUND = new Color(
                darkAwt.getRed(), darkAwt.getGreen(), darkAwt.getBlue(), 200).getRGB();
        Color primaryAwt = new Color(PRIMARY_BLUE, true);
        CATEGORY_TITLE_BORDER = new Color(
                primaryAwt.getRed(), primaryAwt.getGreen(), primaryAwt.getBlue(), 180).getRGB();
        CATEGORY_SEPARATOR_LINE = new Color(
                darkAwt.getRed(), darkAwt.getGreen(), darkAwt.getBlue(), 150).getRGB();

        SCROLLBAR_THUMB = PRIMARY_BLUE;
        MODERN_SCROLLBAR_THUMB_HOVER = PRIMARY_BLUE_BRIGHT;

        CHECKBOX_BOX = COMPONENT_BACKGROUND;
        CHECKBOX_BOX_HOVER = COMPONENT_BACKGROUND_HOVER;
        CHECKBOX_CHECK = PRIMARY_BLUE_BRIGHT;

        DROPDOWN_ARROW = TEXT_PRIMARY;
        DROPDOWN_ITEM_TEXT = TEXT_PRIMARY;
        DROPDOWN_ITEM_HOVER_BG = new Color(
                primaryAwt.getRed(), primaryAwt.getGreen(), primaryAwt.getBlue(), 90).getRGB();
        DROPDOWN_ITEM_SELECTED_BG = new Color(
                primaryAwt.getRed(), primaryAwt.getGreen(), primaryAwt.getBlue(), 130).getRGB();

        SLIDER_TRACK_FILLED = PRIMARY_BLUE;
        SLIDER_KNOB_BLUE_THEME = PRIMARY_BLUE;
        SLIDER_KNOB_HOVER_BLUE_THEME = PRIMARY_BLUE_BRIGHT;
        SLIDER_KNOB = SLIDER_KNOB_BLUE_THEME;
        SLIDER_KNOB_HOVER = SLIDER_KNOB_HOVER_BLUE_THEME;

        TEXTFIELD_BORDER = COMPONENT_BORDER;
        TEXTFIELD_BORDER_FOCUSED = PRIMARY_BLUE_BRIGHT;
        TEXTFIELD_TEXT = TEXT_PRIMARY;

        MODERN_PRIMARY_BACKGROUND = SCREEN_BACKGROUND;
        MODERN_SECONDARY_BACKGROUND = COMPONENT_BACKGROUND;
        MODERN_ACCENT_PRIMARY = PRIMARY_BLUE_BRIGHT;
        MODERN_ACCENT_SECONDARY = PRIMARY_BLUE;
        MODERN_UI_ELEMENT_BORDER = COMPONENT_BORDER;
        MODERN_COMPONENT_HOVER = COMPONENT_BACKGROUND_HOVER;
        MODERN_COMPONENT_ACTIVE = LIGHT_BLUE;
        MODERN_COMPONENT_DISABLED_BG = COMPONENT_BACKGROUND_DISABLED;

        BUTTON_MODERN_BACKGROUND = PRIMARY_BLUE;
        BUTTON_MODERN_BACKGROUND_HOVER = PRIMARY_BLUE_BRIGHT;
        BUTTON_MODERN_TEXT = TEXT_ON_BLUE_BACKGROUND;

        TITLE_BAR_TEXT = TEXT_PRIMARY;
        TITLE_BAR_SEPARATOR = PRIMARY_BLUE_DARK;

        Color screenAwt = new Color(SCREEN_BACKGROUND, true);
        SUBTLE_SHADOW_COLOR = new Color(
                Math.max(0, screenAwt.getRed() - 3),
                Math.max(0, screenAwt.getGreen() - 3),
                Math.max(0, screenAwt.getBlue() - 3),
                120
        ).getRGB();
    }

    public static int getSystemColor(String key) {
        switch (key) {
            case "PRIMARY_BLUE":
                return PRIMARY_BLUE;
            case "PRIMARY_BLUE_BRIGHT":
                return PRIMARY_BLUE_BRIGHT;
            case "PRIMARY_BLUE_DARK":
                return PRIMARY_BLUE_DARK;
            case "LIGHT_BLUE":
                return LIGHT_BLUE;
            case "SCREEN_BACKGROUND":
                return SCREEN_BACKGROUND;
            case "COMPONENT_BACKGROUND":
                return COMPONENT_BACKGROUND;
            case "COMPONENT_BACKGROUND_HOVER":
                return COMPONENT_BACKGROUND_HOVER;
            case "COMPONENT_BACKGROUND_DISABLED":
                return COMPONENT_BACKGROUND_DISABLED;
            case "COMPONENT_BORDER":
                return COMPONENT_BORDER;
            case "COMPONENT_BORDER_BLUE":
                return COMPONENT_BORDER_BLUE;
            case "COMPONENT_BORDER_FOCUSED_BLUE":
                return COMPONENT_BORDER_FOCUSED_BLUE;
            case "TEXT_PRIMARY":
                return TEXT_PRIMARY;
            case "TEXT_SECONDARY":
                return TEXT_SECONDARY;
            case "TEXT_ACCENT":
                return TEXT_ACCENT;
            case "TEXT_DISABLED":
                return TEXT_DISABLED;
            case "TEXT_ON_BLUE_BACKGROUND":
                return TEXT_ON_BLUE_BACKGROUND;
            default:
                return 0;
        }
    }

    public static void setSystemColor(String key, int rgb) {
        switch (key) {
            case "PRIMARY_BLUE":
                PRIMARY_BLUE = rgb;
                break;
            case "PRIMARY_BLUE_BRIGHT":
                PRIMARY_BLUE_BRIGHT = rgb;
                break;
            case "PRIMARY_BLUE_DARK":
                PRIMARY_BLUE_DARK = rgb;
                break;
            case "LIGHT_BLUE":
                LIGHT_BLUE = rgb;
                break;
            case "SCREEN_BACKGROUND":
                SCREEN_BACKGROUND = rgb;
                break;
            case "COMPONENT_BACKGROUND":
                COMPONENT_BACKGROUND = rgb;
                break;
            case "COMPONENT_BACKGROUND_HOVER":
                COMPONENT_BACKGROUND_HOVER = rgb;
                break;
            case "COMPONENT_BACKGROUND_DISABLED":
                COMPONENT_BACKGROUND_DISABLED = rgb;
                break;
            case "COMPONENT_BORDER":
                COMPONENT_BORDER = rgb;
                break;
            case "COMPONENT_BORDER_BLUE":
                COMPONENT_BORDER_BLUE = rgb;
                break;
            case "COMPONENT_BORDER_FOCUSED_BLUE":
                COMPONENT_BORDER_FOCUSED_BLUE = rgb;
                break;
            case "TEXT_PRIMARY":
                TEXT_PRIMARY = rgb;
                break;
            case "TEXT_SECONDARY":
                TEXT_SECONDARY = rgb;
                break;
            case "TEXT_ACCENT":
                TEXT_ACCENT = rgb;
                break;
            case "TEXT_DISABLED":
                TEXT_DISABLED = rgb;
                break;
            case "TEXT_ON_BLUE_BACKGROUND":
                TEXT_ON_BLUE_BACKGROUND = rgb;
                break;
            default:
                return;
        }
        refreshDerived();
    }

    public static int getDefaultSystemColor(String key) {
        switch (key) {
            case "PRIMARY_BLUE":
                return DEFAULT_PRIMARY_BLUE;
            case "PRIMARY_BLUE_BRIGHT":
                return DEFAULT_PRIMARY_BLUE_BRIGHT;
            case "PRIMARY_BLUE_DARK":
                return DEFAULT_PRIMARY_BLUE_DARK;
            case "LIGHT_BLUE":
                return DEFAULT_LIGHT_BLUE;
            case "SCREEN_BACKGROUND":
                return DEFAULT_SCREEN_BACKGROUND;
            case "COMPONENT_BACKGROUND":
                return DEFAULT_COMPONENT_BACKGROUND;
            case "COMPONENT_BACKGROUND_HOVER":
                return DEFAULT_COMPONENT_BACKGROUND_HOVER;
            case "COMPONENT_BACKGROUND_DISABLED":
                return DEFAULT_COMPONENT_BACKGROUND_DISABLED;
            case "COMPONENT_BORDER":
                return DEFAULT_COMPONENT_BORDER;
            case "COMPONENT_BORDER_BLUE":
                return DEFAULT_COMPONENT_BORDER_BLUE;
            case "COMPONENT_BORDER_FOCUSED_BLUE":
                return DEFAULT_COMPONENT_BORDER_FOCUSED_BLUE;
            case "TEXT_PRIMARY":
                return DEFAULT_TEXT_PRIMARY;
            case "TEXT_SECONDARY":
                return DEFAULT_TEXT_SECONDARY;
            case "TEXT_ACCENT":
                return DEFAULT_TEXT_ACCENT;
            case "TEXT_DISABLED":
                return DEFAULT_TEXT_DISABLED;
            case "TEXT_ON_BLUE_BACKGROUND":
                return DEFAULT_TEXT_ON_BLUE_BACKGROUND;
            default:
                return 0;
        }
    }

    public static void loadFromSettings(Settings settings) {
        if (settings == null || settings.guiSystemColors == null || settings.guiSystemColors.length != SYSTEM_COLOR_KEYS.length) {
            applyDefaults();
            return;
        }
        for (int i = 0; i < SYSTEM_COLOR_KEYS.length; i++) {
            setSystemColorWithoutRefresh(SYSTEM_COLOR_KEYS[i], settings.guiSystemColors[i]);
        }
        refreshDerived();
    }

    public static void saveToSettings(Settings settings) {
        if (settings == null) {
            return;
        }
        int[] colors = new int[SYSTEM_COLOR_KEYS.length];
        for (int i = 0; i < SYSTEM_COLOR_KEYS.length; i++) {
            colors[i] = getSystemColor(SYSTEM_COLOR_KEYS[i]);
        }
        settings.guiSystemColors = colors;
    }

    public static Map<String, Integer> snapshotSystemColors() {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        for (String key : SYSTEM_COLOR_KEYS) {
            map.put(key, getSystemColor(key));
        }
        return map;
    }

    public static String toHexRgb(int argb) {
        Color c = new Color(argb, true);
        return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Integer parseHexRgb(String hex) {
        if (hex == null) {
            return null;
        }
        String s = hex.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() != 6) {
            return null;
        }
        try {
            int r = Integer.parseInt(s.substring(0, 2), 16);
            int g = Integer.parseInt(s.substring(2, 4), 16);
            int b = Integer.parseInt(s.substring(4, 6), 16);
            return new Color(r, g, b).getRGB();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void setSystemColorWithoutRefresh(String key, int rgb) {
        switch (key) {
            case "PRIMARY_BLUE":
                PRIMARY_BLUE = rgb;
                break;
            case "PRIMARY_BLUE_BRIGHT":
                PRIMARY_BLUE_BRIGHT = rgb;
                break;
            case "PRIMARY_BLUE_DARK":
                PRIMARY_BLUE_DARK = rgb;
                break;
            case "LIGHT_BLUE":
                LIGHT_BLUE = rgb;
                break;
            case "SCREEN_BACKGROUND":
                SCREEN_BACKGROUND = rgb;
                break;
            case "COMPONENT_BACKGROUND":
                COMPONENT_BACKGROUND = rgb;
                break;
            case "COMPONENT_BACKGROUND_HOVER":
                COMPONENT_BACKGROUND_HOVER = rgb;
                break;
            case "COMPONENT_BACKGROUND_DISABLED":
                COMPONENT_BACKGROUND_DISABLED = rgb;
                break;
            case "COMPONENT_BORDER":
                COMPONENT_BORDER = rgb;
                break;
            case "COMPONENT_BORDER_BLUE":
                COMPONENT_BORDER_BLUE = rgb;
                break;
            case "COMPONENT_BORDER_FOCUSED_BLUE":
                COMPONENT_BORDER_FOCUSED_BLUE = rgb;
                break;
            case "TEXT_PRIMARY":
                TEXT_PRIMARY = rgb;
                break;
            case "TEXT_SECONDARY":
                TEXT_SECONDARY = rgb;
                break;
            case "TEXT_ACCENT":
                TEXT_ACCENT = rgb;
                break;
            case "TEXT_DISABLED":
                TEXT_DISABLED = rgb;
                break;
            case "TEXT_ON_BLUE_BACKGROUND":
                TEXT_ON_BLUE_BACKGROUND = rgb;
                break;
            default:
                break;
        }
    }
}
