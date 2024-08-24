/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.input;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import meteordevelopment.meteorclient.gui.GuiKeyEvents;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.SystemUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.*;

public abstract class WTextBox extends WWidget {
    private static final Renderer DEFAULT_RENDERER = (renderer, x, y, text, color) -> renderer.text(text, x, y, color, false);

    public Runnable action;
    public Runnable actionOnUnfocused;

    protected String text;
    protected String placeholder;
    protected CharFilter filter;

    protected final Renderer renderer;

    protected boolean focused;
    protected DoubleList textWidths = new DoubleArrayList();

    protected int cursor;
    protected double textStart;

    protected boolean selecting;
    protected int selectionStart, selectionEnd;
    private int preSelectionCursor;

    private List<String> completions;
    private int completionsStart;
    private WContainer completionsW;

    public WTextBox(String text, CharFilter filter, Class<? extends Renderer> renderer) {
        this(text, null, filter, renderer);
    }

    public WTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        this.text = text;
        this.placeholder = placeholder;
        this.filter = filter;

        try {
            this.renderer = renderer != null ? renderer.getDeclaredConstructor().newInstance() : DEFAULT_RENDERER;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract WContainer createCompletionsRootWidget();

    protected abstract <T extends WWidget & ICompletionItem> T createCompletionsValueWidth(String completion, boolean selected);

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        double s = theme.textHeight();

        width = pad + s + pad;
        height = pad + s + pad;

        calculateTextWidths();

        if (completionsW != null) completionsW.calculateSize();
    }

    @Override
    public void calculateWidgetPositions() {
        super.calculateWidgetPositions();

        if (completionsW != null) {
            completionsW.x = x;
            completionsW.y = y + height;
            completionsW.calculateWidgetPositions();
        }
    }

    @Override
    public void move(double deltaX, double deltaY) {
        super.move(deltaX, deltaY);
        if (completionsW != null) completionsW.move(deltaX, deltaY);
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
        else if (key == GLFW_KEY_TAB && completionsW != null) {
            String completion = ((ICompletionItem) completionsW.cells.get(getSelectedCompletion()).widget()).getCompletion();

            StringBuilder sb = new StringBuilder(text.length() + completion.length() + 1);
            String a = text.substring(0, cursor);
            sb.append(a);

            for (int i = 0; i < completion.length() - 1; i++) {
                if (a.endsWith(completion.substring(0, completion.length() - i - 1))) {
                    completion = completion.substring(completion.length() - i - 1);
                    break;
                }
            }

            sb.append(completion);
            if (completion.endsWith("(")) sb.append(')');

            sb.append(text, cursor, text.length());

            text = sb.toString();
            cursor += completion.length();
            resetSelection();
            runAction();

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
            sb.append(text);

            for (int i = 0; i < clipboard.length(); i++) {
                char c = clipboard.charAt(i);
                if (filter.filter(sb.toString(), c)) {
                    sb.insert(cursor + addedChars, c);
                    addedChars++;
                }
            }

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
            if (cursor == selectionStart && cursor == selectionEnd) {
                if (cursor < text.length()) {
                    String preText = text;

                    int count = mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_ALT : MinecraftClient.IS_SYSTEM_MAC ? GLFW_MOD_SUPER : GLFW_MOD_CONTROL)
                        ? text.length() - cursor
                        : (mods == (SystemUtils.IS_OS_WINDOWS ? GLFW_MOD_CONTROL : GLFW_MOD_ALT))
                        ? countToNextSpace(false)
                        : 1;

                    text = text.substring(0, cursor) + text.substring(cursor + count);

                    if (!text.equals(preText)) runAction();
                }
            }
            else {
                clearSelection();
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
        else if (key == GLFW_KEY_DOWN && completionsW != null) {
            int currentI = getSelectedCompletion();

            if (currentI == Math.min(5, completions.size() - 1)) {
                if (completionsStart + 6 < completions.size()) {
                    completionsStart++;
                    createCompletions(completionsStart + currentI);
                }
            }
            else {
                ((ICompletionItem) completionsW.cells.get(currentI).widget()).setSelected(false);
                ((ICompletionItem) completionsW.cells.get(currentI + 1).widget()).setSelected(true);
            }

            return true;
        }
        else if (key == GLFW_KEY_UP && completionsW != null) {
            int currentI = getSelectedCompletion();

            if (currentI == 0) {
                if (completionsStart > 0) {
                    completionsStart--;
                    createCompletions(completionsStart + currentI);
                }
            }
            else {
                ((ICompletionItem) completionsW.cells.get(currentI).widget()).setSelected(false);
                ((ICompletionItem) completionsW.cells.get(currentI - 1).widget()).setSelected(true);
            }

            return true;
        }

        return false;
    }

    private int getSelectedCompletion() {
        for (int i = 0; i < completionsW.cells.size(); i++) {
            ICompletionItem item = (ICompletionItem) completionsW.cells.get(i).widget();
            if (!item.isSelected()) continue;

            return i;
        }

        return -1;
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

        if (completionsW != null && focused) {
            renderer.absolutePost(() -> {
                renderer.beginRender();
                completionsW.render(renderer, mouseX, mouseY, delta);
                renderer.endRender();
            });
        }

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

        textStart = MathHelper.clamp(textStart, 0, Math.max(textWidth() - maxTextWidth(), 0));

        onCursorChanged();

        // Completions
        completions = renderer.getCompletions(text, this.cursor);
        completionsStart = 0;
        completionsW = null;
        if (completions != null && !completions.isEmpty()) createCompletions(0);
    }
    protected void onCursorChanged() {}

    private void createCompletions(int selected) {
        completionsW = createCompletionsRootWidget();
        completionsW.theme = theme;

        int max = Math.min(completions.size(), completionsStart + 6);
        for (int i = completionsStart; i < max; i++) {
            WWidget widget = createCompletionsValueWidth(completions.get(i), i == selected);
            widget.theme = theme;

            Cell<?> cell = completionsW.add(widget).expandX().padHorizontal(4);
            if (i == max - 1) cell.padBottom(4);
        }

        completionsW.calculateSize();
        completionsW.x = Math.min(Math.max(x - pad() * 2 + getTextWidth(cursor) - getOverflowWidthForRender(), x), x + width - completionsW.width);
        completionsW.y = y + height;
        completionsW.calculateWidgetPositions();
    }

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

        cursor = MathHelper.clamp(cursor, 0, text.length());
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

    public interface Renderer {
        void render(GuiRenderer renderer, double x, double y, String text, Color color);

        default List<String> getCompletions(String text, int position) {
            return null;
        }
    }

    public interface ICompletionItem {
        boolean isSelected();

        void setSelected(boolean selected);

        String getCompletion();
    }
}
