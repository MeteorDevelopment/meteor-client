/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

import static com.mojang.blaze3d.platform.InputConstants.*;

public class Keybind implements ISerializable<Keybind>, ICopyable<Keybind> {
    private boolean isKey;
    private int value;
    private int modifiers;

    private Keybind(boolean isKey, int value, int modifiers) {
        set(isKey, value, modifiers);
    }

    public static Keybind none() {
        return new Keybind(true, UNKNOWN.getValue(), 0);
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
        return value != UNKNOWN.getValue();
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
        set(true, UNKNOWN.getValue(), 0);
    }

    public boolean canBindTo(boolean isKey, int value, int modifiers) {
        if (isKey) {
            if (modifiers != 0 && isKeyMod(value)) return false;
            return value != UNKNOWN.getValue() && value != KEY_ESCAPE;
        }
        return value != MOUSE_BUTTON_LEFT && value != MOUSE_BUTTON_RIGHT;
    }

    public boolean matches(boolean isKey, int value, int modifiers) {
        if (!this.isSet() || this.isKey != isKey) return false;
        if (!hasMods()) return this.value == value;
        return this.value == value && this.modifiers == modifiers;
    }

    public boolean matches(KeyEvent input) {
        return matches(true, input.key(), input.modifiers());
    }

    public boolean matches(MouseButtonInfo input) {
        return matches(false, input.button(), 0);
    }

    public boolean isPressed() {
        return isKey ? modifiersPressed() && Input.isKeyPressed(value) : Input.isButtonPressed(value);
    }

    private boolean modifiersPressed() {
        if (!hasMods()) return true;

        if (!isModPressed(MOD_CONTROL, KEY_LCONTROL, KEY_RCONTROL)) return false;
        if (!isModPressed(MOD_SUPER, KEY_LSUPER, KEY_RSUPER)) return false;
        if (!isModPressed(MOD_ALT, KEY_LALT, KEY_RALT)) return false;
        if (!isModPressed(MOD_SHIFT, KEY_LSHIFT, KEY_RSHIFT)) return false;

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
        return key >= KEY_LSHIFT && key <= KEY_RSUPER;
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
        if ((modifiers & MOD_CONTROL) != 0) label.append("Ctrl + ");
        if ((modifiers & MOD_SUPER) != 0) label.append("Cmd + ");
        if ((modifiers & MOD_ALT) != 0) label.append("Alt + ");
        if ((modifiers & MOD_SHIFT) != 0) label.append("Shift + ");
        if ((modifiers & MOD_CAPS_LOCK) != 0) label.append("Caps Lock + ");
        if ((modifiers & MOD_NUM_LOCK) != 0) label.append("Num Lock + ");
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
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("isKey", isKey);
        tag.putInt("value", value);
        tag.putInt("modifiers", modifiers);

        return tag;
    }

    @Override
    public Keybind fromTag(CompoundTag tag) {
        isKey = tag.getBooleanOr("isKey", false);
        value = tag.getIntOr("value", 0);
        modifiers = tag.getIntOr("modifiers", 0);

        return this;
    }
}
