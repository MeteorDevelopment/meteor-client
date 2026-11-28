/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.lwjgl.sdl.SDLScancode;

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
                } catch (IllegalArgumentException _) {
                }
            }
        } else {
            // Legacy format: raw ints (GLFW)
            Optional<Boolean> isKeyOpt = tag.getBoolean("isKey");
            Optional<Integer> valueOpt = tag.getInt("value");

            // check for empty config and check for legacy empty keybind (false & 0)
            if (isKeyOpt.isEmpty() || valueOpt.isEmpty() || (!isKeyOpt.get() && valueOpt.get() == 0)) {
                reset();
            } else {
                boolean isKey = isKeyOpt.get();
                int value = isKey
                    ? LegacyMappings.KEY.get(valueOpt.get().intValue())
                    : LegacyMappings.translateMouse(valueOpt.get());

                int mods = tag.getIntOr("modifiers", 0);
                set(isKey ? keyboard(value) : mouse(value), isKey ? Modifier.fromRawValue(LegacyMappings.translateModifiers(mods)) : Set.of());
            }
        }

        return this;
    }

    private static InputConstants.Key keyboard(int key) {
        return InputConstants.Type.KEYBOARD.getOrCreate(key);
    }

    private static InputConstants.Key mouse(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button);
    }

    private static class LegacyMappings {
        private static final Int2IntMap KEY = new Int2IntOpenHashMap();

        static {
            KEY.defaultReturnValue(InputConstants.UNKNOWN.getValue());

            KEY.put(32, InputConstants.KEY_SPACE);
            KEY.put(39, InputConstants.KEY_APOSTROPHE);
            KEY.put(44, InputConstants.KEY_COMMA);
            KEY.put(45, InputConstants.KEY_MINUS);
            KEY.put(46, InputConstants.KEY_PERIOD);
            KEY.put(47, InputConstants.KEY_SLASH);
            KEY.put(48, InputConstants.KEY_0);
            KEY.put(49, InputConstants.KEY_1);
            KEY.put(50, InputConstants.KEY_2);
            KEY.put(51, InputConstants.KEY_3);
            KEY.put(52, InputConstants.KEY_4);
            KEY.put(53, InputConstants.KEY_5);
            KEY.put(54, InputConstants.KEY_6);
            KEY.put(55, InputConstants.KEY_7);
            KEY.put(56, InputConstants.KEY_8);
            KEY.put(57, InputConstants.KEY_9);
            KEY.put(59, InputConstants.KEY_SEMICOLON);
            KEY.put(61, InputConstants.KEY_EQUALS);
            KEY.put(65, InputConstants.KEY_A);
            KEY.put(66, InputConstants.KEY_B);
            KEY.put(67, InputConstants.KEY_C);
            KEY.put(68, InputConstants.KEY_D);
            KEY.put(69, InputConstants.KEY_E);
            KEY.put(70, InputConstants.KEY_F);
            KEY.put(71, InputConstants.KEY_G);
            KEY.put(72, InputConstants.KEY_H);
            KEY.put(73, InputConstants.KEY_I);
            KEY.put(74, InputConstants.KEY_J);
            KEY.put(75, InputConstants.KEY_K);
            KEY.put(76, InputConstants.KEY_L);
            KEY.put(77, InputConstants.KEY_M);
            KEY.put(78, InputConstants.KEY_N);
            KEY.put(79, InputConstants.KEY_O);
            KEY.put(80, InputConstants.KEY_P);
            KEY.put(81, InputConstants.KEY_Q);
            KEY.put(82, InputConstants.KEY_R);
            KEY.put(83, InputConstants.KEY_S);
            KEY.put(84, InputConstants.KEY_T);
            KEY.put(85, InputConstants.KEY_U);
            KEY.put(86, InputConstants.KEY_V);
            KEY.put(87, InputConstants.KEY_W);
            KEY.put(88, InputConstants.KEY_X);
            KEY.put(89, InputConstants.KEY_Y);
            KEY.put(90, InputConstants.KEY_Z);
            KEY.put(91, InputConstants.KEY_LBRACKET);
            KEY.put(92, InputConstants.KEY_BACKSLASH);
            KEY.put(93, InputConstants.KEY_RBRACKET);
            KEY.put(96, InputConstants.KEY_GRAVE);
            KEY.put(161, SDLScancode.SDL_SCANCODE_NONUSBACKSLASH); // WORLD_1 -> key.keyboard.world.1
            KEY.put(162, SDLScancode.SDL_SCANCODE_NONUSHASH);      // WORLD_2 -> key.keyboard.world.2
            KEY.put(256, InputConstants.KEY_ESCAPE);
            KEY.put(257, InputConstants.KEY_RETURN);
            KEY.put(258, InputConstants.KEY_TAB);
            KEY.put(259, InputConstants.KEY_BACKSPACE);
            KEY.put(260, InputConstants.KEY_INSERT);
            KEY.put(261, InputConstants.KEY_DELETE);
            KEY.put(262, InputConstants.KEY_RIGHT);
            KEY.put(263, InputConstants.KEY_LEFT);
            KEY.put(264, InputConstants.KEY_DOWN);
            KEY.put(265, InputConstants.KEY_UP);
            KEY.put(266, InputConstants.KEY_PAGEUP);
            KEY.put(267, InputConstants.KEY_PAGEDOWN);
            KEY.put(268, InputConstants.KEY_HOME);
            KEY.put(269, InputConstants.KEY_END);
            KEY.put(280, InputConstants.KEY_CAPSLOCK);
            KEY.put(281, InputConstants.KEY_SCROLLLOCK);
            KEY.put(282, InputConstants.KEY_NUMLOCK);
            KEY.put(283, InputConstants.KEY_PRINTSCREEN);
            KEY.put(284, InputConstants.KEY_PAUSE);
            KEY.put(290, InputConstants.KEY_F1);
            KEY.put(291, InputConstants.KEY_F2);
            KEY.put(292, InputConstants.KEY_F3);
            KEY.put(293, InputConstants.KEY_F4);
            KEY.put(294, InputConstants.KEY_F5);
            KEY.put(295, InputConstants.KEY_F6);
            KEY.put(296, InputConstants.KEY_F7);
            KEY.put(297, InputConstants.KEY_F8);
            KEY.put(298, InputConstants.KEY_F9);
            KEY.put(299, InputConstants.KEY_F10);
            KEY.put(300, InputConstants.KEY_F11);
            KEY.put(301, InputConstants.KEY_F12);
            KEY.put(302, InputConstants.KEY_F13);
            KEY.put(303, InputConstants.KEY_F14);
            KEY.put(304, InputConstants.KEY_F15);
            KEY.put(305, InputConstants.KEY_F16);
            KEY.put(306, InputConstants.KEY_F17);
            KEY.put(307, InputConstants.KEY_F18);
            KEY.put(308, InputConstants.KEY_F19);
            KEY.put(309, InputConstants.KEY_F20);
            KEY.put(310, InputConstants.KEY_F21);
            KEY.put(311, InputConstants.KEY_F22);
            KEY.put(312, InputConstants.KEY_F23);
            KEY.put(313, InputConstants.KEY_F24);
            // 314 (F25): no SDL scancode
            KEY.put(320, InputConstants.KEY_NUMPAD0);
            KEY.put(321, InputConstants.KEY_NUMPAD1);
            KEY.put(322, InputConstants.KEY_NUMPAD2);
            KEY.put(323, InputConstants.KEY_NUMPAD3);
            KEY.put(324, InputConstants.KEY_NUMPAD4);
            KEY.put(325, InputConstants.KEY_NUMPAD5);
            KEY.put(326, InputConstants.KEY_NUMPAD6);
            KEY.put(327, InputConstants.KEY_NUMPAD7);
            KEY.put(328, InputConstants.KEY_NUMPAD8);
            KEY.put(329, InputConstants.KEY_NUMPAD9);
            KEY.put(330, SDLScancode.SDL_SCANCODE_KP_PERIOD); // KP_DECIMAL is the numpad "." key, not 26.3's keypad.decimal (220)
            KEY.put(331, SDLScancode.SDL_SCANCODE_KP_DIVIDE);
            KEY.put(332, InputConstants.KEY_MULTIPLY);
            KEY.put(333, SDLScancode.SDL_SCANCODE_KP_MINUS);
            KEY.put(334, InputConstants.KEY_ADD);
            KEY.put(335, InputConstants.KEY_NUMPADENTER);
            KEY.put(336, InputConstants.KEY_NUMPADEQUALS);
            KEY.put(340, InputConstants.KEY_LSHIFT);
            KEY.put(341, InputConstants.KEY_LCONTROL);
            KEY.put(342, InputConstants.KEY_LALT);
            KEY.put(343, InputConstants.KEY_LGUI);
            KEY.put(344, InputConstants.KEY_RSHIFT);
            KEY.put(345, InputConstants.KEY_RCONTROL);
            KEY.put(346, InputConstants.KEY_RALT);
            KEY.put(347, InputConstants.KEY_RGUI);
            KEY.put(348, SDLScancode.SDL_SCANCODE_APPLICATION); // MENU is the context-menu key, not 26.3's key.keyboard.menu (118)
        }

        private static int translateMouse(int glfwMouse) {
            // SDLMouse defines up to 5
            // InputConstants defines up to 8
            // Windows & macOS support only 5
            // some linux distros go up to 48
            // i don't care
            return glfwMouse + 1;
        }

        private static int translateModifiers(int glfwMods) {
            int mods = 0;
            if ((glfwMods & 0x01) != 0) mods |= InputConstants.MOD_SHIFT;
            if ((glfwMods & 0x02) != 0) mods |= InputConstants.MOD_CONTROL;
            if ((glfwMods & 0x04) != 0) mods |= InputConstants.MOD_ALT;
            if ((glfwMods & 0x08) != 0) mods |= InputConstants.MOD_SUPER;
            if ((glfwMods & 0x10) != 0) mods |= InputConstants.MOD_CAPS_LOCK;
            if ((glfwMods & 0x20) != 0) mods |= InputConstants.MOD_NUM_LOCK;
            return mods;
        }
    }
}
