package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dropdown extends GuiComponentBase {
    private final List<String> options;
    private int selectedIndex = -1;
    public boolean isOpen = false;
    public final int optionHeight = 24;
    public final int maxDisplayableOptions = 5;
    private float scrollYOptions = 0f;
    private float maxScrollYOptions = 0f;
    private boolean isDraggingScrollbar = false;
    private final int scrollbarWidth = 10;
    private boolean needsScrollbar = false;
    private int lastMouseYForScrollDrag = 0;
    private final float cornerRadius = MODERN_CORNER_RADIUS;
    private final float listCornerRadius = 3f;
    private final int textPaddingX = MODERN_ELEMENT_PADDING_X;
    private final int textPaddingY;
    private final OnSelectionChanged onSelectionChanged;

    public Dropdown(int id, int x, int y, int width, int height, String label,
                    List<String> options, int initialSelectedIndex, OnSelectionChanged onSelectionChanged) {
        super(id, x, y, width, height, label);
        this.options = options != null ? new ArrayList<String>(options) : new ArrayList<String>();
        this.onSelectionChanged = onSelectionChanged;
        this.textPaddingY = (this.height - fontRenderer.FONT_HEIGHT) / 2 + 1;
        int idx = this.options.isEmpty() ? -1 : Math.max(0, Math.min(initialSelectedIndex, this.options.size() - 1));
        setSelected(idx, false);
    }

    public Dropdown(int id, int x, int y, int width, String label,
                    List<String> options, int initialSelectedIndex, OnSelectionChanged onSelectionChanged) {
        this(id, x, y, width, MODERN_DROPDOWN_HEIGHT, label, options, initialSelectedIndex, onSelectionChanged);
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        super.draw(mouseX, mouseY, partialTicks);
        if (!visible) {
            return;
        }

        int mainBoxBg;
        int mainBoxBorder;
        int currentTextColor = enabled ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_DISABLED;
        int arrowColor = enabled ? GuiColors.DROPDOWN_ARROW : GuiColors.TEXT_DISABLED;

        if (!enabled) {
            mainBoxBg = GuiColors.COMPONENT_BACKGROUND_DISABLED;
            mainBoxBorder = GuiColors.MODERN_UI_ELEMENT_BORDER;
        } else if (isOpen) {
            mainBoxBg = GuiColors.COMPONENT_BACKGROUND;
            mainBoxBorder = GuiColors.PRIMARY_BLUE_BRIGHT;
        } else if (this.hovered) {
            mainBoxBg = GuiColors.COMPONENT_BACKGROUND_HOVER;
            mainBoxBorder = GuiColors.PRIMARY_BLUE;
        } else {
            mainBoxBg = GuiColors.COMPONENT_BACKGROUND;
            mainBoxBorder = GuiColors.MODERN_UI_ELEMENT_BORDER;
        }

        float borderThickness = MODERN_BORDER_THICKNESS;

        roundRect(
                x + SHADOW_OFFSET_X, y + SHADOW_OFFSET_Y,
                width, height,
                cornerRadius, GuiColors.SUBTLE_SHADOW_COLOR
        );
        roundRect(x, y, width, height, cornerRadius, mainBoxBorder);
        roundRect(
                x + borderThickness, y + borderThickness,
                width - borderThickness * 2, height - borderThickness * 2,
                Math.max(0f, cornerRadius - borderThickness), mainBoxBg
        );

        String selectedText = selected();
        if (selectedText == null) {
            selectedText = "Select...";
        }
        fontRenderer.drawString(selectedText, x + textPaddingX, y + textPaddingY, currentTextColor);
        String arrow = isOpen ? "\u25B2" : "\u25BC";
        fontRenderer.drawString(arrow, x + width - fontRenderer.getStringWidth(arrow) - textPaddingX, y + textPaddingY, arrowColor);

        topLabel(-3);

        if (isOpen && enabled) {
            int totalContentH = totalHeight();
            int listVisH = listHeight();
            needsScrollbar = totalContentH > listVisH;
            int listTopY = this.y + this.height;
            int listDrawWidth = this.width;

            roundRect(
                    x + SHADOW_OFFSET_X, listTopY + SHADOW_OFFSET_Y,
                    width, listVisH,
                    listCornerRadius, GuiColors.SUBTLE_SHADOW_COLOR
            );
            roundRect(x, listTopY, width, listVisH, listCornerRadius, GuiColors.MODERN_UI_ELEMENT_BORDER);
            roundRect(
                    x + borderThickness, listTopY + borderThickness,
                    width - borderThickness * 2, listVisH - borderThickness * 2,
                    Math.max(0f, listCornerRadius - borderThickness), GuiColors.DROPDOWN_BACKGROUND_OPEN
            );

            ScaledResolution sr = new ScaledResolution(mc);
            int borderIntScissor = (int) borderThickness;
            int scissorListDrawWidth = needsScrollbar ? listDrawWidth - scrollbarWidth : listDrawWidth;

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            int scissorY = sr.getScaledHeight() - (listTopY + listVisH - borderIntScissor);
            GL11.glScissor(
                    (x + borderIntScissor) * sr.getScaleFactor(),
                    scissorY * sr.getScaleFactor(),
                    (scissorListDrawWidth - 2 * borderIntScissor) * sr.getScaleFactor(),
                    (listVisH - 2 * borderIntScissor) * sr.getScaleFactor()
            );

            for (int i = 0; i < options.size(); i++) {
                int optTopAbs = i * optionHeight;
                int optTopScreen = listTopY + optTopAbs - (int) scrollYOptions;

                if (optTopScreen + optionHeight < listTopY || optTopScreen > listTopY + listVisH) {
                    continue;
                }

                int optTextY = optTopScreen + (optionHeight - fontRenderer.FONT_HEIGHT) / 2 + 1;
                boolean isOptHover = mouseX >= x + borderIntScissor && mouseX < x + scissorListDrawWidth - borderIntScissor
                        && mouseY >= Math.max(listTopY + borderIntScissor, optTopScreen)
                        && mouseY < Math.min(listTopY + listVisH - borderIntScissor, optTopScreen + optionHeight);

                int optBg = 0;
                if (isOptHover) {
                    optBg = GuiColors.DROPDOWN_ITEM_HOVER_BG;
                } else if (i == selectedIndex) {
                    optBg = GuiColors.DROPDOWN_ITEM_SELECTED_BG;
                }

                if (optBg != 0) {
                    roundRect(
                            x + borderIntScissor + 1f, optTopScreen,
                            scissorListDrawWidth - 2 * borderIntScissor - 2f, optionHeight,
                            1f, optBg
                    );
                }
                fontRenderer.drawString(options.get(i), x + textPaddingX, optTextY, GuiColors.DROPDOWN_ITEM_TEXT);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            if (needsScrollbar) {
                maxScrollYOptions = Math.max(0f, (float) (totalContentH - listVisH));
                scrollYOptions = Math.max(0f, Math.min(maxScrollYOptions, scrollYOptions));
                int sbX = x + width - scrollbarWidth - borderIntScissor;
                roundRect(
                        sbX,
                        listTopY + borderIntScissor,
                        scrollbarWidth,
                        listVisH - 2 * borderIntScissor,
                        2f, GuiColors.SCROLLBAR_BG
                );

                if (maxScrollYOptions > 0) {
                    float thumbHRatio = Math.max(0.1f, Math.min(1f, listVisH / (float) totalContentH));
                    int thumbH = Math.max(15, (int) (listVisH * thumbHRatio));
                    int trackDrawableHeight = listVisH - 2 * borderIntScissor;
                    float thumbActualY = (listTopY + borderIntScissor)
                            + ((trackDrawableHeight - thumbH) * (scrollYOptions / maxScrollYOptions));

                    boolean thumbHover = mouseX >= sbX && mouseX < sbX + scrollbarWidth
                            && mouseY >= thumbActualY && mouseY < thumbActualY + thumbH;
                    roundRect(
                            sbX + 1f, thumbActualY,
                            scrollbarWidth - 2f, thumbH,
                            2f,
                            thumbHover ? GuiColors.MODERN_SCROLLBAR_THUMB_HOVER : GuiColors.SCROLLBAR_THUMB
                    );
                }
            } else {
                scrollYOptions = 0f;
                maxScrollYOptions = 0f;
            }
        }
    }

    @Override
    public boolean click(int mouseX, int mouseY, int mouseButton) {
        if (!enabled || !visible || mouseButton != 0) {
            return false;
        }

        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            isOpen = !isOpen;
            if (isOpen) {
                if (!options.isEmpty() && selectedIndex != -1 && selectedIndex < options.size()) {
                    int selTop = selectedIndex * optionHeight;
                    int selBot = selTop + optionHeight;
                    int visH = listHeight();
                    if (selTop < scrollYOptions) {
                        scrollYOptions = selTop;
                    } else if (selBot > scrollYOptions + visH) {
                        scrollYOptions = selBot - visH;
                    }

                    if (needsScrollbar) {
                        maxScrollYOptions = Math.max(0f, (float) (totalHeight() - visH));
                        scrollYOptions = Math.max(0f, Math.min(maxScrollYOptions, scrollYOptions));
                    } else {
                        scrollYOptions = 0f;
                    }
                } else {
                    scrollYOptions = 0f;
                }
            }
            mc.getSoundHandler().playSound(
                    PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
            return true;
        }

        if (isOpen) {
            int listTopY = this.y + this.height;
            int listVisibleHeight = listHeight();
            int listBottomY = listTopY + listVisibleHeight;
            int actualListBorderThicknessInt = (int) MODERN_BORDER_THICKNESS;

            if (needsScrollbar) {
                int sbX = x + width - scrollbarWidth - actualListBorderThicknessInt;
                if (mouseX >= sbX && mouseX < sbX + scrollbarWidth
                        && mouseY >= listTopY && mouseY < listBottomY) {
                    isDraggingScrollbar = true;
                    lastMouseYForScrollDrag = mouseY;
                    int trackDrawableHeight = listVisibleHeight - 2 * actualListBorderThicknessInt;
                    if (trackDrawableHeight > 0) {
                        float clickRatioInTrack = (mouseY - (listTopY + actualListBorderThicknessInt))
                                / (float) trackDrawableHeight;
                        scrollYOptions = Math.max(0f, Math.min(maxScrollYOptions, maxScrollYOptions * clickRatioInTrack));
                    }
                    return true;
                }
            }

            int itemsAreaWidth = needsScrollbar ? width - scrollbarWidth : width;
            if (mouseX >= x + actualListBorderThicknessInt
                    && mouseX < x + itemsAreaWidth - actualListBorderThicknessInt
                    && mouseY >= listTopY + actualListBorderThicknessInt
                    && mouseY < listBottomY - actualListBorderThicknessInt) {

                float mouseYInListContent = mouseY - (listTopY + actualListBorderThicknessInt);
                float absoluteMouseYInOptions = mouseYInListContent + scrollYOptions;
                int clickedOptionIndex = (int) (absoluteMouseYInOptions / optionHeight);

                if (clickedOptionIndex >= 0 && clickedOptionIndex < options.size()) {
                    setSelected(clickedOptionIndex, true);
                    isOpen = false;
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 0.8F));
                    return true;
                }
            }

            if (mouseX >= x && mouseX < x + width && mouseY >= listTopY && mouseY < listBottomY) {
                return true;
            }
        }

        if (isOpen) {
            int totalDropdownHeight = this.height + (isOpen ? listHeight() : 0);
            if (!(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + totalDropdownHeight)) {
                close();
            }
        }
        return false;
    }

    public boolean scroll(int rawMouseX, int rawMouseY, int dWheel) {
        if (isOpen && enabled && visible && !options.isEmpty()) {
            int listTopY = y + height;
            int listVisibleH = listHeight();
            int listBottomY = listTopY + listVisibleH;

            if (rawMouseX >= x && rawMouseX < x + width
                    && rawMouseY >= listTopY && rawMouseY < listBottomY) {

                if (totalHeight() <= listVisibleH) {
                    return false;
                }

                maxScrollYOptions = Math.max(0f, (float) (totalHeight() - listVisibleH));
                float scrollAmountPerTick = optionHeight * 1.5f;
                float scrollDelta = dWheel > 0 ? -scrollAmountPerTick : scrollAmountPerTick;

                scrollYOptions = Math.max(0f, Math.min(maxScrollYOptions, scrollYOptions + scrollDelta));
                return true;
            }
        }
        return false;
    }

    @Override
    public void drag(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDraggingScrollbar && clickedMouseButton == 0 && needsScrollbar && maxScrollYOptions > 0) {
            int dy = mouseY - lastMouseYForScrollDrag;
            lastMouseYForScrollDrag = mouseY;

            int trackDrawableHeight = listHeight() - 2 * (int) MODERN_BORDER_THICKNESS;
            if (trackDrawableHeight <= 0) {
                return;
            }

            float thumbHRatio = Math.max(0.1f, Math.min(1f,
                    listHeight() / (float) totalHeight()));
            int thumbH = Math.max(15, (int) (listHeight() * thumbHRatio));
            int draggableTrackSpace = trackDrawableHeight - thumbH;

            if (draggableTrackSpace <= 0) {
                return;
            }

            float scrollAmount = dy * (maxScrollYOptions / (float) draggableTrackSpace);
            scrollYOptions = Math.max(0f, Math.min(maxScrollYOptions, scrollYOptions + scrollAmount));
        }
    }

    @Override
    public void release(int mouseX, int mouseY, int state) {
        if (state == 0) {
            isDraggingScrollbar = false;
        }
    }

    public void close() {
        if (isOpen) {
            isOpen = false;
            isDraggingScrollbar = false;
        }
    }

    public void setSelected(int index, boolean notify) {
        int old = selectedIndex;
        if (options.isEmpty()) {
            selectedIndex = -1;
        } else {
            selectedIndex = Math.max(0, Math.min(index, options.size() - 1));
        }
        if (notify && old != selectedIndex && selectedIndex != -1 && selectedIndex < options.size()
                && onSelectionChanged != null) {
            onSelectionChanged.accept(selectedIndex, options.get(selectedIndex));
        }
    }

    public String selected() {
        if (!options.isEmpty() && selectedIndex != -1 && selectedIndex < options.size()) {
            return options.get(selectedIndex);
        }
        return null;
    }

    public int index() {
        return selectedIndex;
    }

    public List<String> options() {
        return Collections.unmodifiableList(options);
    }

    private int listHeight() {
        return visibleCount() * optionHeight;
    }

    private int totalHeight() {
        return options.size() * optionHeight;
    }

    private int visibleCount() {
        return Math.min(options.size(), maxDisplayableOptions);
    }

    public interface OnSelectionChanged {
        void accept(int index, String option);
    }
}