/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.lang3.SystemUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;
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

    protected boolean selecting;
    protected int selectionStart, selectionEnd;
    private int preSelectionCursor;

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
                    selectionStart = 0;
                    selectionEnd = 0;

                    runAction();
                }
            }
            else if (button == GLFW_MOUSE_BUTTON_LEFT) {
                selecting = true;

                double overflowWidth = getOverflowWidthForRender();
                double relativeMouseX = mouseX - x + overflowWidth;
                double pad = pad();

                double smallestDifference = Double.MAX_VALUE;

                cursor = text.length();

                for (int i = 0; i < textWidths.size(); i++) {
                    double difference = Math.abs(textWidths.getDouble(i) + pad - relativeMouseX);

                    if (difference < smallestDifference) {
                        smallestDifference = difference;
                        cursor = i;
                    }
                }

                preSelectionCursor = cursor;
                resetSelection();
                cursorChanged();
            }

            setFocused(true);
            return true;
        }

        if (focused) setFocused(false);

        return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
        if (!selecting) return;

        double overflowWidth = getOverflowWidthForRender();
        double relativeMouseX = mouseX - x + overflowWidth;
        double pad = pad();

        double smallestDifference = Double.MAX_VALUE;

        for (int i = 0; i < textWidths.size(); i++) {
            double difference = Math.abs(textWidths.getDouble(i) + pad - relativeMouseX);

            if (difference < smallestDifference) {
                smallestDifference = difference;
                if (i < preSelectionCursor) {
                    selectionStart = i;
                    cursor = i;
                }
                else if (i > preSelectionCursor) {
                    selectionEnd = i;
                    cursor = i;
                }
                else {
                    cursor = preSelectionCursor;
                    resetSelection();
                }
            }
        }
    }

    @Override
    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        selecting = false;

        if (selectionStart < preSelectionCursor && preSelectionCursor == selectionEnd) {
            cursor = selectionStart;
        }
        else if (selectionEnd > preSelectionCursor && preSelectionCursor == selectionStart) {
            cursor = selectionEnd;
        }

        return false;
    }

    @Override
    public boolean onKeyPressed(int key, int mods) {
        if (!focused) return false;

        boolean control = MinecraftClient.IS_SYSTEM_MAC ? mods == GLFW_MOD_SUPER : mods == GLFW_MOD_CONTROL;

        if (control && key == GLFW_KEY_C) {
            if (cursor != selectionStart || cursor != selectionEnd) {
                mc.keyboard.setClipboard(text.substring(selectionStart, selectionEnd));
            }
            return true;
        }
        else if (control && key == GLFW_KEY_X) {
            if (cursor != selectionStart || cursor != selectionEnd) {
                mc.keyboard.setClipboard(text.substring(selectionStart, selectionEnd));
                clearSelection();
            }

            return true;
        }
        else if (control && key == GLFW_KEY_A) {
            cursor = text.length();
            selectionStart = 0;
            selectionEnd = cursor;
        }
        else if (mods == ((MinecraftClient.IS_SYSTEM_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL) | GLFW_MOD_SHIFT) && key == GLFW_KEY_A) {
            resetSelection();
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

        boolean control = MinecraftClient.IS_SYSTEM_MAC ? mods == GLFW_MOD_SUPER : mods == GLFW_MOD_CONTROL;
        boolean shift = mods == GLFW_MOD_SHIFT;
        boolean controlShift = mods == ((SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MinecraftClient.IS_SYSTEM_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL) | GLFW_MOD_SHIFT);
        boolean altShift = mods == ((SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT) | GLFW_MOD_SHIFT);

        if (control && key == GLFW_KEY_V) {
            clearSelection();

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
            resetSelection();

            if (!text.equals(preText)) runAction();
            return true;
        }
        else if (key == GLFW_KEY_BACKSPACE) {
            if (cursor > 0 && cursor == selectionStart && cursor == selectionEnd) {
                String preText = text;

                int count = (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MinecraftClient.IS_SYSTEM_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL))
                    ? cursor
                    : (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT))
                    ? countToNextSpace(true)
                    : 1;

                text = text.substring(0, cursor - count) + text.substring(cursor);
                cursor -= count;
                resetSelection();

                if (!text.equals(preText)) runAction();
            }
            else if (cursor != selectionStart || cursor != selectionEnd) {
                clearSelection();
            }

            return true;
        }
        else if (key == GLFW_KEY_DELETE) {
            if (cursor < text.length()) {
                if (cursor == selectionStart && cursor == selectionEnd) {
                    String preText = text;

                    int count = mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MinecraftClient.IS_SYSTEM_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL)
                        ? text.length() - cursor
                        : (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT))
                        ? countToNextSpace(false)
                        : 1;

                    text = text.substring(0, cursor) + text.substring(cursor + count);

                    if (!text.equals(preText)) runAction();
                }
                else {
                    clearSelection();
                }
            }

            return true;
        }
        else if (key == GLFW_KEY_LEFT) {
            if (cursor > 0) {
                if (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT)) {
                    cursor -= countToNextSpace(true);
                    resetSelection();
                }
                else if (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MinecraftClient.IS_SYSTEM_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL)) {
                    cursor = 0;
                    resetSelection();
                }
                else if (altShift) {
                    if (cursor == selectionEnd && cursor != selectionStart) {
                        cursor -= countToNextSpace(true);
                        selectionEnd = cursor;
                    }
                    else {
                        cursor -= countToNextSpace(true);
                        selectionStart = cursor;
                    }
                }
                else if (controlShift) {
                    if (cursor == selectionEnd && cursor != selectionStart) {
                        selectionEnd = selectionStart;
                    }
                    selectionStart = 0;

                    cursor = 0;
                }
                else if (shift) {
                    if (cursor == selectionEnd && cursor != selectionStart) {
                        selectionEnd = cursor - 1;
                    }
                    else {
                        selectionStart = cursor - 1;
                    }

                    cursor--;
                }
                else {
                    if (cursor == selectionEnd && cursor != selectionStart) {
                        cursor = selectionStart;
                    }
                    else {
                        cursor--;
                    }

                    resetSelection();
                }

                cursorChanged();
            }
            else if (selectionStart != selectionEnd && selectionStart == 0 && mods == 0) {
                cursor = 0;
                resetSelection();
                cursorChanged();
            }

            return true;
        }
        else if (key == GLFW_KEY_RIGHT) {
            if (cursor < text.length()) {
                if (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT)) {
                    cursor += countToNextSpace(false);
                    resetSelection();
                }
                else if (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MinecraftClient.IS_SYSTEM_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL)) {
                    cursor = text.length();
                    resetSelection();
                }
                else if (altShift) {
                    if (cursor == selectionStart && cursor != selectionEnd) {
                        cursor += countToNextSpace(false);
                        selectionStart = cursor;
                    }
                    else {
                        cursor += countToNextSpace(false);
                        selectionEnd = cursor;
                    }
                }
                else if (controlShift) {
                    if (cursor == selectionStart && cursor != selectionEnd) {
                        selectionStart = selectionEnd;
                    }
                    cursor = text.length();
                    selectionEnd = cursor;
                }
                else if (shift) {
                    if (cursor == selectionStart && cursor != selectionEnd) {
                        selectionStart = cursor + 1;
                    }
                    else {
                        selectionEnd = cursor + 1;
                    }

                    cursor++;
                }
                else {
                    if (cursor == selectionStart && cursor != selectionEnd) {
                        cursor = selectionEnd;
                    }
                    else {
                        cursor++;
                    }

                    resetSelection();
                }

                cursorChanged();
            }
            else if (selectionStart != selectionEnd && selectionEnd == text.length() && mods == 0) {
                cursor = text.length();
                resetSelection();
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
            clearSelection();

            text = text.substring(0, cursor) + c + text.substring(cursor);

            cursor++;
            resetSelection();

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

    private void clearSelection() {
        if (selectionStart == selectionEnd) return;

        String preText = text;

        text = text.substring(0, selectionStart) + text.substring(selectionEnd);

        cursor = selectionStart;
        selectionEnd = cursor;

        if (!text.equals(preText)) runAction();
    }

    private void resetSelection() {
        selectionStart = cursor;
        selectionEnd = cursor;
    }

    private int countToNextSpace(boolean toLeft) {
        int count = 0;
        boolean hadNonSpace = false;

        for (int i = cursor; toLeft ? i >= 0 : i < text.length(); i += toLeft ? -1 : 1) {
            int j = i;
            if (toLeft) j--;

            if (j >= text.length()) continue;
            if (j < 0) break;

            if (hadNonSpace && Character.isWhitespace(text.charAt(j))) break;
            else if (!Character.isWhitespace(text.charAt(j))) hadNonSpace = true;

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

    protected double getTextWidth(int pos) {
        if (textWidths.isEmpty()) return 0;

        if (pos < 0) pos = 0;
        else if (pos >= textWidths.size()) pos = textWidths.size() - 1;

        return textWidths.getDouble(pos);
    }

    protected double getCursorTextWidth(int offset) {
        return getTextWidth(cursor + offset);
    }

    protected double getOverflowWidthForRender() {
        return textStart;
    }

    public String get() {
        return text;
    }

    public void set(String text) {
        this.text = text;

        cursor = Utils.clamp(cursor, 0, text.length());
        selectionStart = cursor;
        selectionEnd = cursor;

        calculateTextWidths();
        cursorChanged();
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (this.focused && !focused && actionOnUnfocused != null) actionOnUnfocused.run();

        boolean wasJustFocused = focused && !this.focused;

        this.focused = focused;

        resetSelection();

        if (wasJustFocused) onCursorChanged();
    }

    public void setCursorMax() {
        cursor = text.length();
    }
}
