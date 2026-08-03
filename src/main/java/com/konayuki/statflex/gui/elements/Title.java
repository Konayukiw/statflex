package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import com.konayuki.statflex.gui.GuiFonts;

public class Title extends GuiComponentBase {

    public Title(int id, int x, int y, int width, String text) {
        super(id, x, y, width, GuiFonts.getTitle().FONT_HEIGHT + 3, text);
    }

    @Override
    public void drawComponent(int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }
        GuiFonts.getTitle().drawStringWithShadow(label, x, y,
                enabled ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_DISABLED);
    }
}
