/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public class Keybind implements ISerializable<Keybind>, ICopyable<Keybind> {
    public enum Modifier {
        SHIFT(InputConstants.MOD_SHIFT),
        CONTROL(InputConstants.MOD_CONTROL),
        ALT(InputConstants.MOD_ALT),
        SUPER(InputConstants.MOD_SUPER),
        CAPS_LOCK(InputConstants.MOD_CAPS_LOCK),
        NUM_LOCK(InputConstants.MOD_NUM_LOCK),
        ;

        private static final Modifier[] VALUES = values();

        public final int mask;

        Modifier(int mask) {
            this.mask = mask;
        }

        public boolean isKeyPressed() {
            return switch (this) {
                case CONTROL -> Input.isKeyPressed(InputConstants.KEY_LCONTROL) || Input.isKeyPressed(InputConstants.KEY_RCONTROL);
                case SUPER -> Input.isKeyPressed(InputConstants.KEY_LSUPER) || Input.isKeyPressed(InputConstants.KEY_RSUPER);
                case ALT -> Input.isKeyPressed(InputConstants.KEY_LALT) || Input.isKeyPressed(InputConstants.KEY_RALT);
                case SHIFT -> Input.isKeyPressed(InputConstants.KEY_LSHIFT) || Input.isKeyPressed(InputConstants.KEY_RSHIFT);
                case CAPS_LOCK -> Input.isKeyPressed(InputConstants.KEY_CAPSLOCK);
                case NUM_LOCK -> Input.isKeyPressed(InputConstants.KEY_NUMLOCK);
            };
        }

        private static EnumSet<Modifier> fromRawValue(int modifiers) {
            var result = EnumSet.noneOf(Modifier.class);
            for (var mod : VALUES) {
                if ((mod.mask & modifiers) != 0) result.add(mod);
            }
            return result;
        }
    }

    public InputConstants.Key key;
    private final EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);

    private Keybind(InputConstants.Key key, Collection<Modifier> modifiers) {
        this.key = key;
        this.modifiers.addAll(modifiers);
    }

    public static Keybind none() {
        return new Keybind(InputConstants.UNKNOWN, Set.of());
    }

    public static Keybind fromKey(InputConstants.Key key) {
        return new Keybind(key, Set.of());
    }

    public static Keybind fromKey(int key) {
        return fromKey(keyboard(key));
    }

    public static Keybind fromKeys(InputConstants.Key key, Collection<Modifier> modifiers) {
        return new Keybind(key, modifiers);
    }

    public static Keybind fromKeys(InputConstants.Key key, Modifier... modifiers) {
        return fromKeys(key, Arrays.asList(modifiers));
    }

    public static Keybind fromKeys(int key, int modifiers) {
        return fromKeys(keyboard(key), Modifier.fromRawValue(modifiers));
    }

    public static Keybind fromButton(int button) {
        return fromKey(mouse(button));
    }

    public boolean isSet() {
        return !key.equals(InputConstants.UNKNOWN);
    }

    public boolean isKey() {
        return key.getType() != InputConstants.Type.MOUSE;
    }

    public boolean hasMods() {
        return !modifiers.isEmpty();
    }

    public int getValue() {
        return key.getValue();
    }

    public void set(InputConstants.Key key, Collection<Modifier> modifiers) {
        this.key = key;
        this.modifiers.clear();
        this.modifiers.addAll(modifiers);
    }

    public void set(InputConstants.Key key, Modifier... modifiers) {
        set(key, Arrays.asList(modifiers));
    }

    public void set(boolean isKey, int value, int modifiers) {
        set(isKey ? keyboard(value) : mouse(value), isKey ? Modifier.fromRawValue(modifiers) : Set.of());
    }

    @Override
    public Keybind set(Keybind value) {
        set(value.key, value.modifiers);

        return this;
    }

    public void reset() {
        set(InputConstants.UNKNOWN, Set.of());
    }

    public boolean canBindTo(InputConstants.Key key, Collection<Modifier> modifiers) {
        if (key.getType() != InputConstants.Type.MOUSE) {
            if (!modifiers.isEmpty() && isKeyMod(key)) return false;
            return !key.equals(InputConstants.UNKNOWN) && !key.equals(keyboard(InputConstants.KEY_ESCAPE));
        }

        return !key.equals(mouse(InputConstants.MOUSE_BUTTON_LEFT)) && !key.equals(mouse(InputConstants.MOUSE_BUTTON_RIGHT));
    }

    public boolean canBindTo(boolean isKey, int value, int modifiers) {
        return canBindTo(isKey ? keyboard(value) : mouse(value), isKey ? Modifier.fromRawValue(modifiers) : Set.of());
    }

    public boolean matches(InputConstants.Key key, Set<Modifier> modifiers) {
        if (!isSet() || !this.key.equals(key)) return false;
        return !hasMods() || this.modifiers.equals(modifiers);
    }

    public boolean matches(boolean isKey, int value, int modifiers) {
        return matches(isKey ? keyboard(value) : mouse(value), isKey ? Modifier.fromRawValue(modifiers) : Set.of());
    }

    public boolean matches(KeyEvent input) {
        return matches(InputConstants.getKey(input), Modifier.fromRawValue(input.modifiers()));
    }

    public boolean matches(MouseButtonInfo input) {
        return matches(mouse(input.button()), Set.of());
    }

    public boolean isPressed() {
        if (key.getType() == InputConstants.Type.MOUSE) return Input.isButtonPressed(key.getValue());

        return modifiersPressed() && Input.isKeyPressed(key.getValue());
    }

    private boolean modifiersPressed() {
        if (!hasMods()) return true;

        for (Modifier modifier : modifiers) {
            if (!modifier.isKeyPressed()) return false;
        }

        return true;
    }

    private boolean isKeyMod(InputConstants.Key key) {
        return key.getValue() >= InputConstants.KEY_LSHIFT && key.getValue() <= InputConstants.KEY_RSUPER;
    }

    @Override
    public Keybind copy() {
        return new Keybind(key, modifiers);
    }

    @Override
    public String toString() {
        if (!isSet()) return "None";
        if (key.getType() == InputConstants.Type.MOUSE) return key.getDisplayName().getString();
        if (modifiers.isEmpty()) return key.getDisplayName().getString();

        StringBuilder label = new StringBuilder();
        if (modifiers.contains(Modifier.CONTROL)) label.append("Ctrl + ");
        if (modifiers.contains(Modifier.SUPER)) label.append("Cmd + ");
        if (modifiers.contains(Modifier.ALT)) label.append("Alt + ");
        if (modifiers.contains(Modifier.SHIFT)) label.append("Shift + ");
        if (modifiers.contains(Modifier.CAPS_LOCK)) label.append("Caps Lock + ");
        if (modifiers.contains(Modifier.NUM_LOCK)) label.append("Num Lock + ");
        label.append(key.getDisplayName().getString());

        return label.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keybind keybind = (Keybind) o;
        return key.equals(keybind.key) && modifiers.equals(keybind.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, modifiers);
    }

    // Serialization

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putString("key", key.getName());

        ListTag modifiersTag = new ListTag();
        for (Modifier modifier : modifiers) {
            modifiersTag.add(StringTag.valueOf(modifier.name()));
        }
        tag.put("modifiers", modifiersTag);

        return tag;
    }

    @Override
    public Keybind fromTag(CompoundTag tag) {
        if (tag.contains("key")) {
            // New format: key name + modifier enum names
            try {
                key = InputConstants.getKey(tag.getStringOr("key", ""));
            } catch (IllegalArgumentException e) {
                key = InputConstants.UNKNOWN;
            }

            modifiers.clear();
            ListTag modifiersTag = tag.getListOrEmpty("modifiers");
            for (int i = 0; i < modifiersTag.size(); i++) {
                try {
                    modifiers.add(Modifier.valueOf(modifiersTag.getStringOr(i, "")));
                } catch (IllegalArgumentException ignored) {}
            }
        } else {
            // Legacy format: raw ints (GLFW)
            boolean isKey = tag.getBooleanOr("isKey", true);
            int value = tag.getIntOr("value", InputConstants.UNKNOWN.getValue());
            int mods = tag.getIntOr("modifiers", 0);

            set(isKey ? keyboard(value) : mouse(value), isKey ? Modifier.fromRawValue(mods) : Set.of());
        }

        return this;
    }

    private static InputConstants.Key keyboard(int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key);
    }

    private static InputConstants.Key mouse(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button);
    }

}
