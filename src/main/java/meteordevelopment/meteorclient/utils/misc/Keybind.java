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

import java.util.*;

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
                case SUPER -> Input.isKeyPressed(InputConstants.KEY_LGUI) || Input.isKeyPressed(InputConstants.KEY_RGUI);
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
        this.key = Objects.requireNonNull(key, "key");
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
        this.key = Objects.requireNonNull(key, "key");
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
        // Modifiers held for other reasons (sneaking, sprinting, or the bound key being a modifier
        // itself, which sets its own bit on press) must not stop a bind without modifiers from
        // matching. Binds that do have modifiers take precedence, see Modules#onAction.
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
        return key.getValue() >= InputConstants.KEY_LALT && key.getValue() <= InputConstants.KEY_RGUI;
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

        // Keep the modifier names for current versions and write GLFW values for legacy versions.
        ListTag modifierNamesTag = new ListTag();
        for (Modifier modifier : modifiers) {
            modifierNamesTag.add(StringTag.valueOf(modifier.name()));
        }
        tag.put("modifierNames", modifierNamesTag);

        // 1.21.11 and earlier use GLFW key codes, mouse buttons and modifier masks.
        boolean isKey = isKey();
        tag.putBoolean("isKey", isKey);
        tag.putInt("value", toLegacyKeyValue(isKey, key.getValue()));
        tag.putInt("modifiers", toLegacyModifiers());

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
            if (tag.contains("modifierNames")) {
                readModifierNames(tag.getListOrEmpty("modifierNames"));
            } else if (tag.get("modifiers") instanceof ListTag modifiersTag) {
                // Read configs written by the previous 26.3 format.
                readModifierNames(modifiersTag);
            } else {
                // Read the legacy integer bitmask used by 1.21.11 and earlier.
                modifiers.addAll(Modifier.fromRawValue(tag.getIntOr("modifiers", 0)));
            }
        } else {
            // Legacy format: raw GLFW key codes and modifier masks.
            boolean isKey = tag.getBooleanOr("isKey", true);
            int value = tag.getIntOr("value", InputConstants.UNKNOWN.getValue());
            int mods = tag.getIntOr("modifiers", 0);

            set(fromLegacyKey(isKey, value), isKey ? fromLegacyModifiers(mods) : Set.of());
        }

        return this;
    }

    private void readModifierNames(ListTag modifiersTag) {
        for (int i = 0; i < modifiersTag.size(); i++) {
            try {
                modifiers.add(Modifier.valueOf(modifiersTag.getStringOr(i, "")));
            } catch (IllegalArgumentException _) {
            }
        }
    }

    // Initialize the mapping only when a config actually needs legacy conversion.
    // Keybind itself can be loaded before Minecraft's input constants have finished initializing.
    private static final class LegacyKeyMappings {
        private static final Map<Integer, Integer> LEGACY_TO_CURRENT_KEYS = createLegacyToCurrentKeyMap();
        private static final Map<Integer, Integer> CURRENT_TO_LEGACY_KEYS = reverseKeyMap();

        private static Map<Integer, Integer> createLegacyToCurrentKeyMap() {
            Map<Integer, Integer> result = new HashMap<>();

            addLegacyKey(result, 32, "key.keyboard.space");
            addLegacyKey(result, 39, "key.keyboard.apostrophe");
            addLegacyKey(result, 44, "key.keyboard.comma");
            addLegacyKey(result, 45, "key.keyboard.minus");
            addLegacyKey(result, 46, "key.keyboard.period");
            addLegacyKey(result, 47, "key.keyboard.slash");

            for (int digit = 0; digit <= 9; digit++) {
                addLegacyKey(result, 48 + digit, "key.keyboard." + digit);
            }

            addLegacyKey(result, 59, "key.keyboard.semicolon");
            addLegacyKey(result, 61, "key.keyboard.equal");

            for (char letter = 'a'; letter <= 'z'; letter++) {
                addLegacyKey(result, Character.toUpperCase(letter), "key.keyboard." + letter);
            }

            addLegacyKey(result, 91, "key.keyboard.left.bracket");
            addLegacyKey(result, 92, "key.keyboard.backslash");
            addLegacyKey(result, 93, "key.keyboard.right.bracket");
            addLegacyKey(result, 96, "key.keyboard.grave.accent");
            addLegacyKey(result, 161, "key.keyboard.world.1");
            addLegacyKey(result, 162, "key.keyboard.world.2");

            addLegacyKey(result, 256, "key.keyboard.escape");
            addLegacyKey(result, 257, "key.keyboard.enter");
            addLegacyKey(result, 258, "key.keyboard.tab");
            addLegacyKey(result, 259, "key.keyboard.backspace");
            addLegacyKey(result, 260, "key.keyboard.insert");
            addLegacyKey(result, 261, "key.keyboard.delete");
            addLegacyKey(result, 262, "key.keyboard.right");
            addLegacyKey(result, 263, "key.keyboard.left");
            addLegacyKey(result, 264, "key.keyboard.down");
            addLegacyKey(result, 265, "key.keyboard.up");
            addLegacyKey(result, 266, "key.keyboard.page.up");
            addLegacyKey(result, 267, "key.keyboard.page.down");
            addLegacyKey(result, 268, "key.keyboard.home");
            addLegacyKey(result, 269, "key.keyboard.end");

            addLegacyKey(result, 280, "key.keyboard.caps.lock");
            addLegacyKey(result, 281, "key.keyboard.scroll.lock");
            addLegacyKey(result, 282, "key.keyboard.num.lock");
            addLegacyKey(result, 283, "key.keyboard.print.screen");
            addLegacyKey(result, 284, "key.keyboard.pause");

            for (int functionKey = 1; functionKey <= 24; functionKey++) {
                addLegacyKey(result, 289 + functionKey, "key.keyboard.f" + functionKey);
            }

            for (int digit = 0; digit <= 9; digit++) {
                addLegacyKey(result, 320 + digit, "key.keyboard.keypad." + digit);
            }

            // GLFW_KEY_KP_DECIMAL (330) is no longer assignable in Minecraft 26.3.
            addLegacyKey(result, 331, "key.keyboard.keypad.divide");
            addLegacyKey(result, 332, "key.keyboard.keypad.multiply");
            addLegacyKey(result, 333, "key.keyboard.keypad.subtract");
            addLegacyKey(result, 334, "key.keyboard.keypad.add");
            addLegacyKey(result, 335, "key.keyboard.keypad.enter");
            addLegacyKey(result, 336, "key.keyboard.keypad.equal");

            addLegacyKey(result, 340, "key.keyboard.left.shift");
            addLegacyKey(result, 341, "key.keyboard.left.control");
            addLegacyKey(result, 342, "key.keyboard.left.alt");
            addLegacyKey(result, 343, "key.keyboard.left.win");
            addLegacyKey(result, 344, "key.keyboard.right.shift");
            addLegacyKey(result, 345, "key.keyboard.right.control");
            addLegacyKey(result, 346, "key.keyboard.right.alt");
            addLegacyKey(result, 347, "key.keyboard.right.win");
            // GLFW_KEY_MENU maps to SDL's application key, not SDL's Compose/menu key.
            addLegacyKey(result, 348, "key.keyboard.application");

            // GLFW_KEY_F25 (314) was removed as an assignable input in Minecraft 26.3.
            return Map.copyOf(result);
        }

        private static void addLegacyKey(Map<Integer, Integer> map, int legacyValue, String keyName) {
            try {
                InputConstants.Key key = InputConstants.getKey(keyName);
                if (!key.equals(InputConstants.UNKNOWN)) map.put(legacyValue, key.getValue());
            } catch (IllegalArgumentException ignored) {
                // The current Minecraft version does not expose this legacy key.
            }
        }

        private static Map<Integer, Integer> reverseKeyMap() {
            Map<Integer, Integer> result = new HashMap<>();
            Set<Integer> ambiguousKeys = new HashSet<>();

            LEGACY_TO_CURRENT_KEYS.forEach((legacy, current) -> {
                Integer previous = result.putIfAbsent(current, legacy);
                if (previous != null && previous != legacy) ambiguousKeys.add(current);
            });

            ambiguousKeys.forEach(result::remove);
            return Map.copyOf(result);
        }
    }

    private static final int LEGACY_MOD_SHIFT = 1;
    private static final int LEGACY_MOD_CONTROL = 2;
    private static final int LEGACY_MOD_ALT = 4;
    private static final int LEGACY_MOD_SUPER = 8;
    private static final int LEGACY_MOD_CAPS_LOCK = 16;
    private static final int LEGACY_MOD_NUM_LOCK = 32;

    private static final int LEGACY_UNKNOWN_KEY = -1;

    private static InputConstants.Key fromLegacyKey(boolean isKey, int value) {
        if (!isKey) {
            // GLFW: left=0, right=1, middle=2. SDL: left=1, middle=2, right=3.
            return switch (value) {
                case 0 -> mouse(InputConstants.MOUSE_BUTTON_LEFT);
                case 1 -> mouse(InputConstants.MOUSE_BUTTON_RIGHT);
                case 2 -> mouse(InputConstants.MOUSE_BUTTON_MIDDLE);
                default -> value >= 3 && value <= 15 ? mouse(value + 1) : InputConstants.UNKNOWN;
            };
        }

        if (value < 0) return InputConstants.UNKNOWN;
        return keyboard(LegacyKeyMappings.LEGACY_TO_CURRENT_KEYS.getOrDefault(value, InputConstants.UNKNOWN.getValue()));
    }

    private static int toLegacyKeyValue(boolean isKey, int value) {
        if (!isKey) {
            // GLFW supports up to 16 mouse buttons; buttons outside that range have no legacy value.
            return switch (value) {
                case InputConstants.MOUSE_BUTTON_LEFT -> 0;
                case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
                case InputConstants.MOUSE_BUTTON_MIDDLE -> 2;
                default -> value >= 4 && value <= 16 ? value - 1 : LEGACY_UNKNOWN_KEY;
            };
        }

        return LegacyKeyMappings.CURRENT_TO_LEGACY_KEYS.getOrDefault(value, LEGACY_UNKNOWN_KEY);
    }

    private static EnumSet<Modifier> fromLegacyModifiers(int modifiers) {
        EnumSet<Modifier> result = EnumSet.noneOf(Modifier.class);
        if ((modifiers & LEGACY_MOD_SHIFT) != 0) result.add(Modifier.SHIFT);
        if ((modifiers & LEGACY_MOD_CONTROL) != 0) result.add(Modifier.CONTROL);
        if ((modifiers & LEGACY_MOD_ALT) != 0) result.add(Modifier.ALT);
        if ((modifiers & LEGACY_MOD_SUPER) != 0) result.add(Modifier.SUPER);
        if ((modifiers & LEGACY_MOD_CAPS_LOCK) != 0) result.add(Modifier.CAPS_LOCK);
        if ((modifiers & LEGACY_MOD_NUM_LOCK) != 0) result.add(Modifier.NUM_LOCK);
        return result;
    }

    private int toLegacyModifiers() {
        int result = 0;
        if (modifiers.contains(Modifier.SHIFT)) result |= LEGACY_MOD_SHIFT;
        if (modifiers.contains(Modifier.CONTROL)) result |= LEGACY_MOD_CONTROL;
        if (modifiers.contains(Modifier.ALT)) result |= LEGACY_MOD_ALT;
        if (modifiers.contains(Modifier.SUPER)) result |= LEGACY_MOD_SUPER;
        if (modifiers.contains(Modifier.CAPS_LOCK)) result |= LEGACY_MOD_CAPS_LOCK;
        if (modifiers.contains(Modifier.NUM_LOCK)) result |= LEGACY_MOD_NUM_LOCK;
        return result;
    }

    private static InputConstants.Key keyboard(int key) {
        return InputConstants.Type.KEYBOARD.getOrCreate(key);
    }

    private static InputConstants.Key mouse(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button);
    }

}
