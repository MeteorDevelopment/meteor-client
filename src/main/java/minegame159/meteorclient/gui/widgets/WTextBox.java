/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.widgets;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.GuiKeyEvents;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.renderer.Region;
import minegame159.meteorclient.utils.misc.CursorStyle;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class WTextBox extends WWidget {
    public Runnable action;

    private String text;
    private final DoubleList textWidths = new DoubleArrayList();
    private int cursor = 0;
    private int fixedCursor = -1;

    private final double uWidth;
    private boolean focused;

    private double cursorTimer;
    private boolean cursorVisible;

    private GuiRenderer renderer;

    private boolean changed;

    public WTextBox(String text, double width) {
        this.text = text;
        this.uWidth = width;
    }

    public void setText(String text) {
        if (this.text.equals(text)) return;

        this.text = text;
        cursor = -1;
        if (renderer != null) calculateTextWidths();
        changed = true;
    }

    public String getText() {
        return text;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (!this.focused && focused) GuiKeyEvents.setPostKeyEvents(true);
        else if (this.focused && !focused) GuiKeyEvents.setPostKeyEvents(false);

        this.focused = focused;
    }

    public int getCursor() {
        return cursor;
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        double s = GuiConfig.get().guiScale;
        width = 6 * s + uWidth * s + 6 * s;
        height = 6 * s + renderer.textHeight() + 6 * s;
    }

    @Override
    protected boolean onMouseClicked(boolean used, int button) {
        if (!focused && mouseOver) GuiKeyEvents.setPostKeyEvents(true);
        else if (focused && !mouseOver) GuiKeyEvents.setPostKeyEvents(false);

        if (!used) {
            focused = mouseOver;

            if (focused && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                String preText = text;
                text = "";
                if (!text.equals(preText)) {
                    changed = false;
                    callActionOnTextChanged();
                    if (!changed) calculateTextWidths();
                }

                resetCursorTimer();
                return true;
            }
        } else {
            if (!mouseOver) focused = false;
        }

        return false;
    }

    @Override
    protected boolean onKeyPressed(int key, int mods) {
        if (focused) {
            if (key == GLFW.GLFW_KEY_V && (mods == GLFW.GLFW_MOD_CONTROL || mods == GLFW.GLFW_MOD_SUPER)) {
                String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                int addedChars = 0;

                StringBuilder sb = new StringBuilder(text.length() + clipboard.length());
                sb.append(text, 0, cursor);
                for (int i = 0; i < clipboard.length(); i++) {
                    char c = clipboard.charAt(i);
                    if (addChar(c)) {
                        sb.append(c);
                        addedChars++;
                    }
                }
                sb.append(text, cursor, text.length());
                text = sb.toString();

                changed = false;
                callActionOnTextChanged();
                if (!changed) {
                    fixedCursor = cursor + addedChars;
                    calculateTextWidths();
                }

                resetCursorTimer();
                return true;
            } else if (key == GLFW.GLFW_KEY_C && (mods == GLFW.GLFW_MOD_CONTROL || mods == GLFW.GLFW_MOD_SUPER)) {
                MinecraftClient.getInstance().keyboard.setClipboard(text);
                return true;
            } else if (key == GLFW.GLFW_KEY_X && (mods == GLFW.GLFW_MOD_CONTROL || mods == GLFW.GLFW_MOD_SUPER)) {
                MinecraftClient.getInstance().keyboard.setClipboard(text);
                String preText = text;
                text = "";

                if (!preText.equals(text)) {
                    changed = false;
                    callActionOnTextChanged();
                    if (!changed) calculateTextWidths();
                }

                resetCursorTimer();
                return true;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && (mods == GLFW.GLFW_MOD_CONTROL || mods == GLFW.GLFW_MOD_SUPER)) {
                String preText = text;

                int lengthToDelete = 0;
                boolean hadSpace = false;
                for (int i = text.length() - 1; i >= 0; i--) {
                    if (hadSpace) break;
                    if (text.charAt(i) == ' ') hadSpace = true;
                    lengthToDelete++;
                }
                text = text.substring(0, text.length() - lengthToDelete);

                if (!preText.equals(text)) {
                    changed = false;
                    callActionOnTextChanged();
                    if (!changed) calculateTextWidths();
                }

                resetCursorTimer();
                return true;
            }

            return onKeyPressedOrRepeated(key);
        }

        return false;
    }

    @Override
    protected boolean onKeyRepeated(int key, int mods) {
        if (focused) {
            return onKeyPressedOrRepeated(key);
        }

        return false;
    }

    private boolean onKeyPressedOrRepeated(int key) {
        if (key == GLFW.GLFW_KEY_LEFT) {
            cursor--;
            if (cursor < 0) cursor = 0;
            resetCursorTimer();
            return true;
        } else if (key == GLFW.GLFW_KEY_RIGHT) {
            cursor++;
            if (cursor > text.length()) cursor = text.length();
            resetCursorTimer();
            return true;
        } else if (key == GLFW.GLFW_KEY_BACKSPACE && cursor > 0) {
            text = text.substring(0, cursor - 1) + text.substring(cursor);

            changed = false;
            callActionOnTextChanged();
            if (!changed) {
                fixedCursor = cursor - 1;
                calculateTextWidths();
            }

            resetCursorTimer();
            return true;
        } else if (key == GLFW.GLFW_KEY_DELETE && cursor < text.length()) {
            text = text.substring(0, cursor) + text.substring(cursor + 1);

            changed = false;
            callActionOnTextChanged();
            if (!changed) {
                fixedCursor = cursor;
                calculateTextWidths();
            }

            resetCursorTimer();
            return true;
        }

        return false;
    }

    @Override
    protected boolean onCharTyped(char c, int key) {
        if (focused) {
            if (addChar(c)) {
                text = text.substring(0, cursor) + c + text.substring(cursor);

                changed = false;
                callActionOnTextChanged();
                if (!changed) {
                    fixedCursor = cursor + 1;
                    calculateTextWidths();
                }

                resetCursorTimer();
            }

            return true;
        }

        return false;
    }

    protected boolean addChar(char c) {
        return true;
    }

    @Override
    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return;
        if (this.renderer == null) {
            this.renderer = renderer;
            calculateTextWidths();
        }
        this.renderer = renderer;
        tooltip = text;
        super.render(renderer, mouseX, mouseY, delta);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (mouseOver) renderer.setCursorStyle(CursorStyle.Type);

        renderer.background(this, false, false);

        if (cursorTimer >= 1) {
            cursorVisible = !cursorVisible;
            cursorTimer = 0;
        } else {
            cursorTimer += delta / 8;
        }

        if (fixedCursor != -1) calculateTextWidths();

        double overflowWidth = getCursorTextWidthForPreview() - width + 16;
        if (overflowWidth < 0) overflowWidth = 0;

        double s = GuiConfig.get().guiScale;

        if (!text.isEmpty()) {
            renderer.beginScissor(x + 6 * s, y + 6 * s, width - 12 * s, height - 12 * s);
            renderer.text(text, x + 6 * s - overflowWidth, y + 6 * s, false, GuiConfig.get().text);
            renderer.endScissor();
        }

        if (focused && cursorVisible) {
            renderer.quad(Region.FULL, x + 6 * s + getCursorTextWidth() - overflowWidth, y + 6 * s, 1, renderer.textHeight(), GuiConfig.get().text);
        }
    }

    private void resetCursorTimer() {
        cursorTimer = 0;
        cursorVisible = true;
    }

    protected void callActionOnTextChanged() {
        if (action != null) action.run();
    }

    private void calculateTextWidths() {
        if (renderer == null) return;
        textWidths.clear();

        for (int i = 0; i <= text.length(); i++) {
            textWidths.add(renderer.textWidth(text, i));
        }

        if (fixedCursor != -1) {
            cursor = fixedCursor;
            fixedCursor = -1;
        } else {
            cursor = text.length();
        }
    }

    private double getCursorTextWidth() {
        if (textWidths.isEmpty()) return 0;
        return textWidths.getDouble(cursor);
    }

    private double getCursorTextWidthForPreview() {
        if (textWidths.isEmpty()) return 0;
        int c = cursor + 2;
        if (c > text.length()) c = text.length();
        return textWidths.getDouble(c);
    }
}
