/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.nbt.NbtCompound;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;

public class Keybind implements ISerializable<Keybind>, ICopyable<Keybind> {
    private boolean isKey;
    private int value;
    private int modifiers;

    private Keybind(boolean isKey, int value, int modifiers) {
        set(isKey, value, modifiers);
    }

    public static Keybind none() {
        return new Keybind(true, GLFW_KEY_UNKNOWN, 0);
    }

    public static Keybind fromKey(int key) {
        return new Keybind(true, key, 0);
    }

    public static Keybind fromKeys(int key, int modifiers) {
        return new Keybind(true, key, modifiers);
    }

    public static Keybind fromButton(int button) {
        return new Keybind(false, button, 0);
    }

    public int getValue() {
        return value;
    }

    public boolean isSet() {
        return value != GLFW_KEY_UNKNOWN;
    }

    public boolean isKey() {
        return isKey;
    }

    public boolean hasMods() {
        return isKey && modifiers != 0;
    }

    public void set(boolean isKey, int value, int modifiers) {
        this.isKey = isKey;
        this.value = value;
        this.modifiers = modifiers;
    }

    @Override
    public Keybind set(Keybind value) {
        this.isKey = value.isKey;
        this.value = value.value;
        this.modifiers = value.modifiers;

        return this;
    }

    public void reset() {
        set(true, GLFW_KEY_UNKNOWN, 0);
    }

    public boolean canBindTo(boolean isKey, int value, int modifiers) {
        if (isKey) {
            if (modifiers != 0 && isKeyMod(value)) return false;
            return value != GLFW_KEY_UNKNOWN && value != GLFW_KEY_ESCAPE;
        }
        return value != GLFW_MOUSE_BUTTON_LEFT && value != GLFW_MOUSE_BUTTON_RIGHT;
    }

    public boolean matches(boolean isKey, int value, int modifiers) {
        if (!this.isSet() || this.isKey != isKey) return false;
        if (!hasMods()) return this.value == value;
        return this.value == value && this.modifiers == modifiers;
    }

    public boolean isPressed() {
        return isKey ? modifiersPressed() && Input.isKeyPressed(value) : Input.isButtonPressed(value);
    }

    private boolean modifiersPressed() {
        if (!hasMods()) return true;

        if (!isModPressed(GLFW_MOD_CONTROL, GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL)) return false;
        if (!isModPressed(GLFW_MOD_SUPER, GLFW_KEY_LEFT_SUPER, GLFW_KEY_RIGHT_SUPER)) return false;
        if (!isModPressed(GLFW_MOD_ALT, GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT)) return false;
        if (!isModPressed(GLFW_MOD_SHIFT, GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT)) return false;

        return true;
    }

    private boolean isModPressed(int value, int... keys) {
        if ((modifiers & value) == 0) return true;

        for (int key : keys) {
            if (Input.isKeyPressed(key)) return true;
        }

        return false;
    }

    private boolean isKeyMod(int key) {
        return key >= GLFW_KEY_LEFT_SHIFT && key <= GLFW_KEY_RIGHT_SUPER;
    }

    @Override
    public Keybind copy() {
        return new Keybind(isKey, value, modifiers);
    }

    @Override
    public String toString() {
        if (!isSet()) return "None";
        if (!isKey) return Utils.getButtonName(value);
        if (modifiers == 0) return Utils.getKeyName(value);

        StringBuilder label = new StringBuilder();
        if ((modifiers & GLFW_MOD_CONTROL) != 0) label.append("Ctrl + ");
        if ((modifiers & GLFW_MOD_SUPER) != 0) label.append("Cmd + ");
        if ((modifiers & GLFW_MOD_ALT) != 0) label.append("Alt + ");
        if ((modifiers & GLFW_MOD_SHIFT) != 0) label.append("Shift + ");
        if ((modifiers & GLFW_MOD_CAPS_LOCK) != 0) label.append("Caps Lock + ");
        if ((modifiers & GLFW_MOD_NUM_LOCK) != 0) label.append("Num Lock + ");
        label.append(Utils.getKeyName(value));

        return label.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keybind keybind = (Keybind) o;
        return isKey == keybind.isKey && value == keybind.value && modifiers == keybind.modifiers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isKey, value, modifiers);
    }

    // Serialization

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.putBoolean("isKey", isKey);
        tag.putInt("value", value);
        tag.putInt("modifiers", modifiers);

        return tag;
    }

    @Override
    public Keybind fromTag(NbtCompound tag) {
        isKey = tag.getBoolean("isKey");
        value = tag.getInt("value");
        modifiers = tag.getInt("modifiers");

        return this;
    }
}
