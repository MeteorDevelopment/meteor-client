/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets.input;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import minegame159.meteorclient.gui.GuiKeyEvents;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.utils.CharFilter;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.Utils;

import static minegame159.meteorclient.utils.Utils.mc;
import static org.lwjgl.glfw.GLFW.*;

public abstract class WTextBox extends WWidget {
    public Runnable action;
    public Runnable actionOnUnfocused;

    protected String text;
    protected CharFilter filter;

    protected boolean focused;
    protected DoubleList textWidths = new DoubleArrayList();

    protected int cursor;
    protected double textStart;

    public WTextBox(String text, CharFilter filter) {
        this.text = text;
        this.filter = filter;
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        double s = theme.textHeight();

        width = pad + s + pad;
        height = pad + s + pad;

        calculateTextWidths();
    }

    protected double maxTextWidth() {
        return width - pad() * 2;
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button, boolean used) {
        if (mouseOver && !used) {
            if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                if (!text.isEmpty()) {
                    text = "";
                    cursor = 0;

                    runAction();
                }
            }
            else {
                double overflowWidth = getOverflowWidthForRender();
                double relativeMouseX = mouseX - x + overflowWidth;
                double pad = pad();

                double smallestDifference = Double.MAX_VALUE;
                cursor = 0;

                for (int i = 0; i < textWidths.size(); i++) {
                    double difference = Math.abs(textWidths.getDouble(i) + pad - relativeMouseX);

                    if (difference < smallestDifference) {
                        smallestDifference = difference;
                        cursor = i;
                    }
                }

                cursorChanged();
            }

            setFocused(true);
            return true;
        }

        if (focused) {
            setFocused(false);
        }

        return false;
    }

    @Override
    public boolean onKeyPressed(int key, int mods) {
        if (!focused) return false;

        boolean controlPressed = mods == GLFW_MOD_CONTROL || mods == GLFW_MOD_SUPER;

        if (key == GLFW_KEY_V && controlPressed) {
            String preText = text;

            String clipboard = mc.keyboard.getClipboard();
            int addedChars = 0;

            StringBuilder sb = new StringBuilder(text.length() + clipboard.length());
            sb.append(text, 0, cursor);
            for (int i = 0; i < clipboard.length(); i++) {
                char c = clipboard.charAt(i);
                if (filter.filter(text, c)) {
                    sb.append(c);
                    addedChars++;
                }
            }
            sb.append(text, cursor, text.length());

            text = sb.toString();
            cursor += addedChars;

            if (!text.equals(preText)) runAction();
            return true;
        }
        else if (key == GLFW_KEY_C && controlPressed) {
            mc.keyboard.setClipboard(text);
            return true;
        }
        else if (key == GLFW_KEY_X && controlPressed) {
            String preText = text;

            mc.keyboard.setClipboard(text);

            text = "";
            cursor = 0;

            if (!text.equals(preText)) runAction();
            return true;
        }
        else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
            setFocused(false);

            if (actionOnUnfocused != null) actionOnUnfocused.run();
            return true;
        }

        return onKeyRepeated(key, mods);
    }

    @Override
    public boolean onKeyRepeated(int key, int mods) {
        if (!focused) return false;

        boolean controlPressed = mods == GLFW_MOD_CONTROL || mods == GLFW_MOD_SUPER;
        boolean shiftPressed = mods == GLFW_MOD_SHIFT;

        if (key == GLFW_KEY_BACKSPACE) {
            if (cursor > 0) {
                String preText = text;

                int count = controlPressed ? countToNextSpace(true) : 1;

                text = text.substring(0, cursor - count) + text.substring(cursor);
                cursor -= count;

                if (!text.equals(preText)) runAction();
            }

            return true;
        }
        else if (key == GLFW_KEY_DELETE) {
            if (cursor < text.length()) {
                String preText = text;

                int count = controlPressed ? countToNextSpace(false) : 1;
                text = text.substring(0, cursor) + text.substring(cursor + count);

                if (!text.equals(preText)) runAction();
            }

            return true;
        }
        else if (key == GLFW_KEY_LEFT || (shiftPressed && key == GLFW_KEY_KP_4)) {
            if (cursor > 0) {
                cursor -= controlPressed ? countToNextSpace(true) : 1;
                cursorChanged();
            }

            return true;
        }
        else if (key == GLFW_KEY_RIGHT || (shiftPressed && key == GLFW_KEY_KP_6)) {
            if (cursor < text.length()) {
                cursor += controlPressed ? countToNextSpace(false) : 1;
                cursorChanged();
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(char c) {
        if (!focused) return false;

        if (filter.filter(text, c)) {
            text = text.substring(0, cursor) + c + text.substring(cursor);
            cursor++;

            runAction();
            return true;
        }

        return false;
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (isFocused()) GuiKeyEvents.canUseKeys = false;

        return super.render(renderer, mouseX, mouseY, delta);
    }

    private int countToNextSpace(boolean toLeft) {
        int count = 0;
        boolean hadNonSpace = false;

        for (int i = cursor; toLeft ? i >= 0 : i < text.length(); i += toLeft ? -1 : 1) {
            int j = i;
            if (toLeft) j--;

            if (j >= text.length()) continue;
            if (j < 0) break;

            if (hadNonSpace && text.charAt(j) == ' ') break;
            else if (text.charAt(j) != ' ') hadNonSpace = true;

            count++;
        }

        return count;
    }

    private void calculateTextWidths() {
        textWidths.clear();

        for (int i = 0; i <= text.length(); i++) {
            textWidths.add(theme.textWidth(text, i, false));
        }
    }

    private void runAction() {
        calculateTextWidths();
        cursorChanged();

        if (action != null) action.run();
    }

    private double textWidth() {
        return textWidths.isEmpty() ? 0 : textWidths.getDouble(textWidths.size() - 1);
    }

    private void cursorChanged() {
        double cursor = getCursorTextWidth(-2);
        if (cursor < textStart) {
            textStart -= textStart - cursor;
        }

        cursor = getCursorTextWidth(2);
        if (cursor > textStart + maxTextWidth()) {
            textStart += cursor - (textStart + maxTextWidth());
        }

        textStart = Utils.clamp(textStart, 0, Math.max(textWidth() - maxTextWidth(), 0));

        onCursorChanged();
    }
    protected void onCursorChanged() {}

    protected double getCursorTextWidth(int offset) {
        if (textWidths.isEmpty()) return 0;

        int i = cursor + offset;
        if (i < 0) i = 0;
        else if (i >= textWidths.size()) i = textWidths.size() - 1;

        return textWidths.getDouble(i);
    }

    protected double getCursorTextWidth() {
        return getCursorTextWidth(0);
    }

    protected double getOverflowWidthForRender() {
        return textStart;
    }

    public String get() {
        return text;
    }

    public void set(String text) {
        this.text = text;
        this.cursor = Utils.clamp(cursor, 0, text.length());

        calculateTextWidths();
        cursorChanged();
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (this.focused && !focused && actionOnUnfocused != null) actionOnUnfocused.run();

        this.focused = focused;
    }
}
