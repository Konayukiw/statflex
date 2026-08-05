package com.konayuki.statflex.gui.elements;

import com.konayuki.statflex.gui.GuiColors;
import net.minecraft.client.gui.GuiTextField;

import java.util.function.Predicate;

public class Text extends GuiComponentBase {
    private final int horizontalTextPadding = MODERN_ELEMENT_PADDING_X;
    private final int verticalTextPadding;
    public final GuiTextField textField;
    private String lastText;
    private boolean hasInitialFocusCallbackFired = false;
    private final float cornerRadius = MODERN_CORNER_RADIUS;
    private final Predicate<String> validator;
    private final OnTextChanged onTextChanged;
    private final OnFocusChanged onFocusChanged;

    public Text(int id, int x, int y, int width, int height, String label, String initialText,
                Predicate<String> validator, OnTextChanged onTextChanged, OnFocusChanged onFocusChanged) {
        super(id, x, y, width, height, label);
        this.verticalTextPadding = (this.height - fontRenderer.FONT_HEIGHT) / 2 + 1;
        this.validator = validator != null ? validator : s -> true;
        this.onTextChanged = onTextChanged;
        this.onFocusChanged = onFocusChanged != null ? onFocusChanged : f -> {};
        this.lastText = initialText != null ? initialText : "";

        textField = new GuiTextField(
                id,
                fontRenderer,
                this.x + horizontalTextPadding,
                this.y + verticalTextPadding,
                this.width - (2 * horizontalTextPadding),
                fontRenderer.FONT_HEIGHT
        );
        textField.setText(this.lastText);
        textField.setMaxStringLength(256);
        textField.setEnableBackgroundDrawing(false);
        textField.setTextColor(GuiColors.TEXTFIELD_TEXT);
        textField.setDisabledTextColour(GuiColors.TEXT_DISABLED);
        textField.setFocused(false);
    }

    public Text(int id, int x, int y, int width, String label, String initialText,
                OnTextChanged onTextChanged, OnFocusChanged onFocusChanged) {
        this(id, x, y, width, MODERN_TEXT_INPUT_HEIGHT, label, initialText, s -> true, onTextChanged, onFocusChanged);
    }

    public Text(int id, int x, int y, int width, String label, String initialText,
                OnTextChanged onTextChanged) {
        this(id, x, y, width, label, initialText, onTextChanged, f -> {});
    }

    @Override
    public void draw(int mouseX, int mouseY, float partialTicks) {
        super.draw(mouseX, mouseY, partialTicks);
        if (!visible) {
            return;
        }

        textField.setEnabled(this.enabled);
        textField.xPosition = this.x + horizontalTextPadding;
        textField.yPosition = this.y + verticalTextPadding;
        textField.width = this.width - (2 * horizontalTextPadding);

        int currentBgColor;
        int currentOuterBorderColor;

        if (!enabled) {
            currentBgColor = GuiColors.COMPONENT_BACKGROUND_DISABLED;
            currentOuterBorderColor = GuiColors.MODERN_UI_ELEMENT_BORDER;
        } else if (textField.isFocused()) {
            currentBgColor = GuiColors.TEXTFIELD_BACKGROUND;
            currentOuterBorderColor = GuiColors.TEXTFIELD_BORDER_FOCUSED;
        } else {
            currentBgColor = GuiColors.TEXTFIELD_BACKGROUND;
            currentOuterBorderColor = GuiColors.TEXTFIELD_BORDER;
        }

        if (textField.isFocused() && enabled) {
            roundRect(
                    x - 1f, y - 1f,
                    width + 2f, height + 2f,
                    cornerRadius + 1f,
                    GuiColors.PRIMARY_BLUE_BRIGHT_GLOW_EFFECT
            );
        }

        float borderThickness = MODERN_BORDER_THICKNESS;

        roundRect(x, y, width, height, cornerRadius, currentOuterBorderColor);
        roundRect(
                x + borderThickness, y + borderThickness,
                width - 2 * borderThickness, height - 2 * borderThickness,
                Math.max(0f, cornerRadius - borderThickness),
                currentBgColor
        );
        textField.setTextColor(enabled ? GuiColors.TEXTFIELD_TEXT : GuiColors.TEXT_DISABLED);
        textField.drawTextBox();

        topLabel(-3);
    }

    @Override
    public boolean click(int mouseX, int mouseY, int mouseButton) {
        if (!visible) {
            if (textField.isFocused()) {
                setFocused(false);
            }
            return false;
        }

        boolean wasFocused = textField.isFocused();
        boolean clickedOnThisComponent = mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;

        if (clickedOnThisComponent) {
            if (enabled) {
                textField.mouseClicked(mouseX, mouseY, mouseButton);
            }
        } else {
            if (textField.isFocused()) {
                setFocused(false);
            }
        }

        if (enabled && textField.isFocused() != wasFocused) {
            onFocusChanged.accept(textField.isFocused());
        }

        return enabled && clickedOnThisComponent;
    }

    @Override
    public boolean key(char typedChar, int keyCode) {
        if (!enabled || !visible || !textField.isFocused()) {
            return false;
        }

        String previousText = textField.getText();
        boolean handled = textField.textboxKeyTyped(typedChar, keyCode);

        if (handled && !textField.getText().equals(previousText)) {
            if (validator.test(textField.getText())) {
                if (onTextChanged != null) {
                    onTextChanged.accept(textField.getText());
                }
                lastText = textField.getText();
            } else {
                textField.setText(previousText);
            }
        }
        return handled;
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String newText, boolean notify) {
        String oldText = textField.getText();
        if (validator.test(newText)) {
            textField.setText(newText);
            if (notify && !newText.equals(oldText) && onTextChanged != null) {
                onTextChanged.accept(newText);
            }
            lastText = newText;
        }
    }

    public void setFocused(boolean isFocused) {
        if (!enabled && isFocused) {
            return;
        }

        boolean oldFocusState = textField.isFocused();
        textField.setFocused(isFocused);

        if (oldFocusState != isFocused || !hasInitialFocusCallbackFired) {
            onFocusChanged.accept(isFocused);
            hasInitialFocusCallbackFired = true;
        }
    }

    public boolean isFocused() {
        return textField.isFocused();
    }

    public void unfocus() {
        if (textField.isFocused()) {
            setFocused(false);
        }
    }

    public interface OnTextChanged {
        void accept(String text);
    }

    public interface OnFocusChanged {
        void accept(boolean focused);
    }
}