/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements.keyboard;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.mixin.KeyBindingAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class KeyboardHud extends HudElement {
    public static final HudElementInfo<KeyboardHud> INFO = new HudElementInfo<>(Hud.GROUP, "keyboard", "Displays pressed keys.", KeyboardHud::new);

    public enum Alignment {
        Left,
        Center,
        Right
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColor = settings.createGroup("Color");
    private final SettingGroup sgBorder = settings.createGroup("Border");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Preset> preset = sgGeneral.add(new EnumSetting.Builder<Preset>()
        .name("preset")
        .description("Which keys to display.")
        .defaultValue(Preset.Movement)
        .onChanged(this::onPresetChanged)
        .build()
    );

    private final Setting<List<Key>> customKeys = sgGeneral.add(new CustomKeyListSetting());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the keyboard.")
        .defaultValue(1.5)
        .min(0.5)
        .sliderRange(0.5, 5)
        .decimalPlaces(1)
        .onChanged(s -> calculateSize())
        .build()
    );

    private final Setting<Double> spacing = sgGeneral.add(new DoubleSetting.Builder()
        .name("spacing")
        .description("Spacing between keys.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 10)
        .decimalPlaces(1)
        .visible(() -> preset.get() != Preset.Custom)
        .onChanged(s -> onPresetChanged(preset.get()))
        .build()
    );

    private final Setting<KeyboardLayout> keyboardLayout = sgGeneral.add(new EnumSetting.Builder<KeyboardLayout>()
        .name("keyboard-layout")
        .description("Physical keyboard layout (ANSI or ISO).")
        .defaultValue(KeyboardLayout.ANSI)
        .visible(() -> preset.get() == Preset.Keyboard)
        .onChanged(layout -> onPresetChanged(preset.get()))
        .build()
    );

    private final Setting<Boolean> showCps = sgGeneral.add(new BoolSetting.Builder()
        .name("Show CPS")
        .description("Shows clicks per second on keys.")
        .defaultValue(false)
        .visible(() -> preset.get() != Preset.Custom)
        .onChanged(b -> onPresetChanged(preset.get()))
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal alignment of the text.")
        .defaultValue(Alignment.Center)
        .build()
    );

    private final Setting<SettingColor> pressedColor = sgColor.add(new ColorSetting.Builder()
        .name("pressed-color")
        .description("Color of pressed keys.")
        .defaultValue(new SettingColor(200, 200, 200, 100))
        .build()
    );

    private final Setting<SettingColor> unpressedColor = sgColor.add(new ColorSetting.Builder()
        .name("unpressed-color")
        .description("Color of unpressed keys.")
        .defaultValue(new SettingColor(0, 0, 0, 100))
        .build()
    );

    private final Setting<Boolean> colorFade = sgColor.add(new BoolSetting.Builder()
        .name("color-fade")
        .description("Whether to fade the key color when pressing/unpressing.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> fadeTime = sgColor.add(new DoubleSetting.Builder()
        .name("fade-time")
        .description("How long to fade the color for, in seconds.")
        .visible(colorFade::get)
        .defaultValue(0.1)
        .min(0.01)
        .sliderRange(0.01, 0.5)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<SettingColor> textColor = sgColor.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the key name.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<Boolean> border = sgBorder.add(new BoolSetting.Builder()
        .name("border")
        .description("Draw a border around keys.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> borderColor = sgBorder.add(new ColorSetting.Builder()
        .name("border-color")
        .description("Color of the key border.")
        .visible(border::get)
        .defaultValue(new SettingColor(255, 255, 255, 200))
        .build()
    );

    private final Setting<Double> borderWidth = sgBorder.add(new DoubleSetting.Builder()
        .name("border-width")
        .description("Width of the key border.")
        .visible(border::get)
        .defaultValue(1.0)
        .min(0.5)
        .sliderRange(0.5, 5)
        .build()
    );

    private final Setting<Double> opacity = sgColor.add(new DoubleSetting.Builder()
        .name("opacity")
        .description("Opacity of the whole element.")
        .defaultValue(1)
        .min(0)
        .max(1)
        .sliderMax(1)
        .build()
    );

    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
        .name("background")
        .description("Displays background.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Color used for the background.")
        .visible(background::get)
        .defaultValue(new SettingColor(25, 25, 25, 50))
        .build()
    );

    private final List<Key> keys = new ArrayList<>();
    private double minX, minY;

    private Color getColor(SettingColor color, SettingColor out) {
        out.set(color);
        out.a = (int) (out.a * opacity.get());
        return out;
    }

    private Color getKeyColor(Key key, SettingColor out) {
        if (colorFade.get()) {
            boolean pressed = key.isPressed;
            float target = pressed ? 1 : 0;

            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getDynamicDeltaTicks();
            float frameDelta = (float) (tickDelta / 20 / fadeTime.get()) * (pressed ? 1 : -1);
            key.delta = Math.clamp(key.delta + frameDelta, 0, 1);

            if (key.delta == target) {
                out.set(target == 1 ? pressedColor.get() : unpressedColor.get());
            } else {
                Color c1 = pressedColor.get();
                Color c2 = unpressedColor.get();

                float[] hsb1 = new float[3];
                float[] hsb2 = new float[3];

                java.awt.Color.RGBtoHSB(c1.r, c1.g, c1.b, hsb2);
                java.awt.Color.RGBtoHSB(c2.r, c2.g, c2.b, hsb1);

                int rgb = java.awt.Color.HSBtoRGB(
                    MathHelper.lerp(key.delta, hsb1[0], hsb2[0]),
                    MathHelper.lerp(key.delta, hsb1[1], hsb2[1]),
                    MathHelper.lerp(key.delta, hsb1[2], hsb2[2])
                );

                out.r = Color.toRGBAR(rgb);
                out.g = Color.toRGBAG(rgb);
                out.b = Color.toRGBAB(rgb);
                out.a = MathHelper.lerp(key.delta, pressedColor.get().a, unpressedColor.get().a);
            }
        } else {
            out.set(key.isPressed ? pressedColor.get() : unpressedColor.get());
        }

        out.a = (int) (out.a * opacity.get());
        return out;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyEvent event) {
        for (Key key : keys) {
            if (key.matches(event.input.key(), event.input.scancode(), true)) {
                key.update(event.action);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onMouseClick(MouseClickEvent event) {
        for (Key key : keys) {
            if (key.matches(event.input.button(), -1, false)) {
                key.update(event.action);
            }
        }
    }

    public KeyboardHud() {
        super(INFO);
        if (mc.options != null)
            onPresetChanged(preset.get());
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private void onPresetChanged(Preset preset) {
        if (mc.options == null)
            return;

        keys.clear();
        double u = 35;  // base key unit size
        double g = spacing.get() * 2;  // gap between keys (spacing setting)
        LayoutContext l = new LayoutContext(u, g, 15);

        switch (preset) {
            case Movement -> {
                keys.add(l.key(mc.options.forwardKey, l.ux(1), 0).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.leftKey, 0, l.y(1)).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.backKey, l.ux(1), l.y(1)).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.rightKey, l.ux(2), l.y(1)).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.sneakKey, 0, l.y(2)).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.jumpKey, l.ux(1), l.y(2), KeyDimensions.UNIT_2U).setShowCps(showCps.get()));
            }
            case Clicks -> {
                keys.add(l.key(mc.options.attackKey, "LMB", 0, 0).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.useKey, "RMB", l.ux(1), 0).setShowCps(showCps.get()));
            }
            case Actions -> {
                keys.add(l.key(mc.options.dropKey, 0, 0).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.swapHandsKey, l.ux(1), 0).setShowCps(showCps.get()));
                keys.add(l.key(mc.options.inventoryKey, l.ux(2), 0).setShowCps(showCps.get()));
            }
            case Hotbar -> {
                for (int i = 0; i < 9; i++) {
                    keys.add(l.key(mc.options.hotbarKeys[i], l.ux(i), 0).setShowCps(showCps.get()));
                }
            }
            case Keyboard -> {
                if (keyboardLayout.get() == KeyboardLayout.ANSI) {
                    buildAnsiLayout(l);
                } else {
                    buildIsoLayout(l);
                }
                for (Key key : keys) key.setShowCps(showCps.get());
            }
            case Custom -> keys.addAll(customKeys.get());
        }

        calculateSize();
    }

    private void buildAnsiLayout(LayoutContext l) {
        double row0 = l.uy(0), row1 = l.uy(1), row2 = l.uy(2), row3 = l.uy(3), row4 = l.uy(4), row5 = l.uy(5);

        // Row 0: ESC, F1-F12, Print/Scroll/Pause
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_ESCAPE), 0, row0));
        for (int i = 0; i < 4; i++)
            keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_F1 + i), l.ux(2d + i), row0));
        for (int i = 0; i < 4; i++)
            keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_F5 + i), l.ux(6.5 + i), row0));
        for (int i = 0; i < 4; i++)
            keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_F9 + i), l.ux(11d + i), row0));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PRINT_SCREEN), l.ux(15.5), row0));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_SCROLL_LOCK), l.ux(16.5), row0));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PAUSE), l.ux(17.5), row0));

        // Row 1: ` 1-0 - = BS, Ins/Home/PgUp
        int[] row1Keys = {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL};
        for (int i = 0; i < row1Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row1Keys[i]), l.ux(i), row1));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSPACE), l.ux(13), row1, KeyDimensions.BACKSPACE));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_INSERT), l.ux(15.5), row1));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_HOME), l.ux(16.5), row1));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_UP), l.ux(17.5), row1));

        // Row 2: Tab QWERTY..., Del/End/PgDn
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_TAB), 0, row2, KeyDimensions.TAB));
        int[] row2Keys = {GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET};
        double tabEnd = l.px(KeyDimensions.TAB) + l.keyGap;
        for (int i = 0; i < row2Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row2Keys[i]), tabEnd + l.ux(i), row2));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSLASH), tabEnd + l.ux(12), row2, KeyDimensions.TAB));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_DELETE), l.ux(15.5), row2));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_END), l.ux(16.5), row2));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_DOWN), l.ux(17.5), row2));

        // Row 3: Caps ASDF..., Enter
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_CAPS_LOCK), 0, row3, KeyDimensions.CAPS_LOCK));
        int[] row3Keys = {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE};
        double capsEnd = l.px(KeyDimensions.CAPS_LOCK) + l.keyGap;
        for (int i = 0; i < row3Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row3Keys[i]), capsEnd + l.ux(i), row3));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_ENTER), capsEnd + l.ux(11), row3, KeyDimensions.ENTER_ANSI));

        // Row 4: LShift ZXCV..., RShift, Up
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SHIFT), 0, row4, KeyDimensions.LEFT_SHIFT_ANSI));
        int[] row4Keys = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH};
        double lShiftEnd = l.px(KeyDimensions.LEFT_SHIFT_ANSI) + l.keyGap;
        for (int i = 0; i < row4Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row4Keys[i]), lShiftEnd + l.ux(i), row4));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SHIFT), lShiftEnd + l.ux(10), row4, KeyDimensions.RIGHT_SHIFT));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_UP), l.ux(16.5), row4));

        // Row 5: Ctrl/Win/Alt/Space/Alt/Win/Menu/Ctrl, Arrows
        double xPos = 0;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_CONTROL), xPos, row5, KeyDimensions.CTRL));
        xPos += l.px(KeyDimensions.CTRL) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SUPER), xPos, row5, KeyDimensions.GUI));
        xPos += l.px(KeyDimensions.GUI) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_ALT), xPos, row5, KeyDimensions.ALT));
        xPos += l.px(KeyDimensions.ALT) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_SPACE), xPos, row5, KeyDimensions.SPACEBAR));
        xPos += l.px(KeyDimensions.SPACEBAR) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_ALT), xPos, row5, KeyDimensions.ALT));
        xPos += l.px(KeyDimensions.ALT) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SUPER), xPos, row5, KeyDimensions.GUI));
        xPos += l.px(KeyDimensions.GUI) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_MENU), xPos, row5, KeyDimensions.MENU));
        xPos += l.px(KeyDimensions.MENU) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_CONTROL), xPos, row5, KeyDimensions.CTRL));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT), l.ux(15.5), row5));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_DOWN), l.ux(16.5), row5));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT), l.ux(17.5), row5));
    }

    private void buildIsoLayout(LayoutContext l) {
        double row0 = l.uy(0), row1 = l.uy(1), row2 = l.uy(2), row3 = l.uy(3), row4 = l.uy(4), row5 = l.uy(5);

        // Row 0: ESC, F1-F12, Print/Scroll/Pause
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_ESCAPE), 0, row0));
        for (int i = 0; i < 4; i++)
            keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_F1 + i), l.ux(2d + i), row0));
        for (int i = 0; i < 4; i++)
            keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_F5 + i), l.ux(6.5 + i), row0));
        for (int i = 0; i < 4; i++)
            keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_F9 + i), l.ux(11d + i), row0));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PRINT_SCREEN), l.ux(15.5), row0));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_SCROLL_LOCK), l.ux(16.5), row0));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PAUSE), l.ux(17.5), row0));

        // Row 1: ` 1-0 - = BS, Ins/Home/PgUp
        int[] row1Keys = {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL};
        for (int i = 0; i < row1Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row1Keys[i]), l.ux(i), row1));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSPACE), l.ux(13), row1, KeyDimensions.BACKSPACE));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_INSERT), l.ux(15.5), row1));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_HOME), l.ux(16.5), row1));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_UP), l.ux(17.5), row1));

        // Row 2: Tab QWERTY... brackets
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_TAB), 0, row2, KeyDimensions.TAB));
        int[] row2Keys = {GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET};
        double tabEnd = l.px(KeyDimensions.TAB) + l.keyGap;
        for (int i = 0; i < row2Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row2Keys[i]), tabEnd + l.ux(i), row2));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_DELETE), l.ux(15.5), row2));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_END), l.ux(16.5), row2));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_DOWN), l.ux(17.5), row2));

        // Row 3: Caps ASDF..., ISO # key
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_CAPS_LOCK), 0, row3, KeyDimensions.CAPS_LOCK));
        int[] row3Keys = {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE};
        double capsEnd = l.px(KeyDimensions.CAPS_LOCK) + l.keyGap;
        for (int i = 0; i < row3Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row3Keys[i]), capsEnd + l.ux(i), row3));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSLASH), capsEnd + l.ux(11), row3));

        // ISO Enter
        double topBarStartX = tabEnd + l.ux(12);
        double mainBlockRightEdge = tabEnd + l.ux(12) + l.px(KeyDimensions.TAB);
        double enterStemWidth = l.px(KeyDimensions.ENTER_ISO_WIDTH);
        double enterStemHeight = l.px(KeyDimensions.ENTER_ISO_HEIGHT);
        double enterStemX = mainBlockRightEdge - enterStemWidth;
        keys.add(new IsoEnterKey(Keybind.fromKey(GLFW.GLFW_KEY_ENTER), enterStemX, row2, enterStemWidth, enterStemHeight, topBarStartX));

        // Row 4: LShift, ISO \| key, ZXCV..., RShift, Up
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SHIFT), 0, row4, KeyDimensions.LEFT_SHIFT_ISO));
        double lShiftEnd = l.px(KeyDimensions.LEFT_SHIFT_ISO) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_WORLD_2), lShiftEnd, row4));

        int[] row4Keys = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH};
        for (int i = 0; i < row4Keys.length; i++)
            keys.add(l.key(Keybind.fromKey(row4Keys[i]), lShiftEnd + l.ux(1d + i), row4));

        double rShiftX = lShiftEnd + l.ux(11);
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SHIFT), rShiftX, row4, KeyDimensions.RIGHT_SHIFT));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_UP), l.ux(16.5), row4));

        // Row 5: Ctrl/Win/Alt/Space/AltGr/Win/Menu/Ctrl, Arrows
        double xPos = 0;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_CONTROL), xPos, row5, KeyDimensions.CTRL));
        xPos += l.px(KeyDimensions.CTRL) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SUPER), xPos, row5, KeyDimensions.GUI));
        xPos += l.px(KeyDimensions.GUI) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_ALT), xPos, row5, KeyDimensions.ALT));
        xPos += l.px(KeyDimensions.ALT) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_SPACE), xPos, row5, KeyDimensions.SPACEBAR));
        xPos += l.px(KeyDimensions.SPACEBAR) + l.keyGap;
        keys.add(l.keyNamed(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_ALT), "AltGr", xPos, row5, KeyDimensions.ALT));
        xPos += l.px(KeyDimensions.ALT) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SUPER), xPos, row5, KeyDimensions.GUI));
        xPos += l.px(KeyDimensions.GUI) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_MENU), xPos, row5, KeyDimensions.MENU));
        xPos += l.px(KeyDimensions.MENU) + l.keyGap;
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_CONTROL), xPos, row5, KeyDimensions.CTRL));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT), l.ux(15.5), row5));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_DOWN), l.ux(16.5), row5));
        keys.add(l.key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT), l.ux(17.5), row5));
    }

    private void calculateSize() {
        if (keys.isEmpty()) {
            setSize(0, 0);
            return;
        }

        minX = minY = 0;
        double maxX = 0, maxY = 0;

        for (Key key : keys) {
            minX = Math.min(minX, key.x);
            minY = Math.min(minY, key.y);
            maxX = Math.max(maxX, key.x + key.width);
            maxY = Math.max(maxY, key.y + key.height);
        }

        setSize((maxX - minX) * scale.get(), (maxY - minY) * scale.get());
    }

    private static String getShortName(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "LEFT SHIFT", "LSHIFT" -> "LSh";
            case "RIGHT SHIFT", "RSHIFT" -> "RSh";
            case "LEFT CONTROL", "LCTRL" -> "LCtrl";
            case "RIGHT CONTROL", "RCTRL" -> "RCtrl";
            case "LEFT ALT", "LALT" -> "LAlt";
            case "RIGHT ALT", "RALT" -> "RAlt";
            case "LEFT SUPER" -> "LSup";
            case "RIGHT SUPER" -> "RSup";
            case "GRAVE ACCENT" -> "`";
            case "COMMA" -> ",";
            case "DOT", "PERIOD" -> ".";
            case "SLASH" -> "/";
            case "APOSTROPHE" -> "'";
            case "BACKSPACE" -> "BS";
            case "ENTER" -> "Ent";
            case "SCROLL", "SCROLL LOCK" -> "ScrL";
            case "PRINT", "PRTSC", "PRINT SCREEN" -> "PrtS";
            case "PAUSE" -> "Paus";
            case "PAGEUP", "PAGE UP", "PGUP" -> "PgUp";
            case "PAGEDOWN", "PAGE DOWN", "PGDN" -> "PgDn";
            case "INSERT", "INS" -> "Ins";
            case "DELETE", "DEL" -> "Del";
            case "HOME" -> "Home";
            case "END" -> "End";
            case "ARROW UP", "UP" -> "Up";
            case "ARROW DOWN", "DOWN" -> "Dn";
            case "ARROW LEFT", "LEFT" -> "Lt";
            case "ARROW RIGHT", "RIGHT" -> "Rt";
            case "WORLD 1" -> "#";
            case "WORLD 2" -> "\\";
            case "UNKNOWN" -> "?";
            default -> name;
        };
    }

    @Override
    public void render(HudRenderer renderer) {
        if (keys.isEmpty()) {
            if (mc.options != null) {
                onPresetChanged(preset.get());
            }
            return;
        }

        SettingColor mutableColor = new SettingColor();
        pressedColor.get().update();
        unpressedColor.get().update();
        textColor.get().update();
        borderColor.get().update();
        backgroundColor.get().update();

        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), getColor(backgroundColor.get(), mutableColor));
        }

        double s = scale.get();

        // because of a meteor bug, the modules search field swallows inputs
        InputUtil.Key guiKey = ((KeyBindingAccessor) KeyBinds.OPEN_GUI).meteor$getKey();

        for (Key key : keys) {
            if (key.matches(guiKey.getCode(), guiKey.getCode(), guiKey.getCategory() != InputUtil.Type.MOUSE)) {
                if (key.isPressed != key.isNativelyPressed()) {
                    key.update(key.isPressed ? KeyAction.Release : KeyAction.Press);
                }
            }

            if (key instanceof IsoEnterKey isoEnter) {
                isoEnter.render(this, renderer, s, mutableColor);
                continue;
            }

            Color color = getKeyColor(key, mutableColor);

            double kX = x + (key.x - minX) * s;
            double kY = y + (key.y - minY) * s;
            double kW = key.width * s;
            double kH = key.height * s;

            renderer.quad(kX, kY, kW, kH, color);

            if (border.get()) {
                Color bColor = getColor(borderColor.get(), mutableColor);
                double bw = borderWidth.get();
                renderer.quad(kX, kY, kW, bw, bColor);
                renderer.quad(kX, kY + kH - bw, kW, bw, bColor);
                renderer.quad(kX, kY, bw, kH, bColor);
                renderer.quad(kX + kW - bw, kY, bw, kH, bColor);
            }

            String text = key.getName();
            Color txtColor = getColor(textColor.get(), mutableColor);

            double padding = 2 * s;
            double availableWidth = kW - padding;

            if (!key.showCps) {
                double textScale = Math.min(1.0, availableWidth / renderer.textWidth(text, 1.0));
                double textWidth = renderer.textWidth(text, textScale);
                double yText = kY + (kH - renderer.textHeight(false, textScale)) / 2;
                drawTextLine(renderer, text, textWidth, kX, yText, kW, textScale, txtColor);
            } else {
                double topScale = Math.min(1.0, availableWidth / renderer.textWidth(text, 1.0));
                double topWidth = renderer.textWidth(text, topScale);
                double topHeight = renderer.textHeight(false, topScale);

                String cpsText = key.getCps() + " CPS";
                double botScale = Math.min(1.0, availableWidth / renderer.textWidth(cpsText, 1.0));
                double botWidth = renderer.textWidth(cpsText, botScale);
                double botHeight = renderer.textHeight(false, botScale);

                double totalHeight = topHeight + botHeight;
                double startY = kY + (kH - totalHeight) / 2;

                drawTextLine(renderer, text, topWidth, kX, startY, kW, topScale, txtColor);
                drawTextLine(renderer, cpsText, botWidth, kX, startY + topHeight, kW, botScale, txtColor);
            }
        }
    }

    private void drawTextLine(HudRenderer renderer, String text, double textWidth, double x, double y, double w, double textScale, Color color) {
        double s = scale.get();
        double padding = 2 * s;

        double xText = x + (w - textWidth) / 2;
        if (alignment.get() == Alignment.Left)
            xText = x + padding;
        else if (alignment.get() == Alignment.Right)
            xText = x + w - padding - textWidth;

        renderer.text(text, xText, y, color, false, textScale);
    }

    public enum Preset {
        Movement,
        Clicks,
        Actions,
        Hotbar,
        Keyboard,
        Custom
    }

    public enum KeyboardLayout {
        ANSI,
        ISO
    }

    public static class Key {
        public String name = "";
        public KeyBinding binding;
        public Keybind keybind;
        public double x, y, width, height;
        public boolean showCps = false;

        private final RollingCps rollingCps = new RollingCps();
        private boolean isPressed;
        private float delta;

        public Key() {
            this.keybind = Keybind.fromKey(GLFW.GLFW_KEY_SPACE);
            this.width = 60;
            this.height = 40;
        }

        public Key(NbtCompound compound) {
            this.keybind = Keybind.none().fromTag(compound.getCompoundOrEmpty("key"));
            this.name = compound.getString("name", "");
            this.x = compound.getDouble("x", 0);
            this.y = compound.getDouble("y", 0);
            this.width = compound.getDouble("width", 60);
            this.height = compound.getDouble("height", 60);
            this.showCps = compound.getBoolean("showCps", false);
        }

        Key(KeyBinding binding, String name, double x, double y, double width, double height) {
            this.binding = binding;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        Key(Keybind keybind, String name, double x, double y, double width, double height) {
            this.keybind = keybind;
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Key setShowCps(boolean show) {
            this.showCps = show;
            return this;
        }

        public String getName() {
            if (name != null && !name.isEmpty()) return name;
            if (keybind != null) return getShortName(keybind.toString());
            if (binding != null) return getShortName(binding.getBoundKeyLocalizedText().getString());
            return "?";
        }

        public boolean matches(int input, int scancode, boolean key) {
            if (keybind != null) {
                return keybind.isKey() == key && keybind.getValue() == input;
            } else {
                InputUtil.Key inputKey = ((KeyBindingAccessor) binding).meteor$getKey();
                boolean isKey = inputKey.getCategory() != InputUtil.Type.MOUSE;
                return isKey == key && inputKey.getCategory() == InputUtil.Type.SCANCODE
                    ? scancode == inputKey.getCode()
                    : input == inputKey.getCode();
            }
        }

        public void update(KeyAction action) {
            if (action != KeyAction.Release) {
                isPressed = true;
                if (showCps && action == KeyAction.Press) {
                    rollingCps.add();
                }
            } else {
                isPressed = false;
            }
        }

        public boolean isNativelyPressed() {
            long window = mc.getWindow().getHandle();
            if (keybind != null) {
                if (!keybind.isSet()) return false;
                return keybind.isKey()
                    ? GLFW.glfwGetKey(window, keybind.getValue()) != GLFW.GLFW_RELEASE
                    : GLFW.glfwGetMouseButton(window, keybind.getValue()) != GLFW.GLFW_RELEASE;
            } else {
                int key = ((KeyBindingAccessor) binding).meteor$getKey().getCode();
                return key >= 0 && key < 8
                    ? GLFW.glfwGetMouseButton(window, key) != GLFW.GLFW_RELEASE
                    : GLFW.glfwGetKey(window, key) != GLFW.GLFW_RELEASE;
            }
        }

        public int getCps() {
            return rollingCps.get();
        }

        public NbtCompound serialize() {
            NbtCompound compound = new NbtCompound();
            compound.put("key", keybind.toTag());
            compound.putString("name", name);
            compound.putDouble("x", x);
            compound.putDouble("y", y);
            compound.putDouble("width", width);
            compound.putDouble("height", height);
            compound.putBoolean("showCps", showCps);
            return compound;
        }
    }

    public static class IsoEnterKey extends Key {
        private final double topBarStartX;

        public IsoEnterKey(Keybind keybind, double x, double y, double width, double height, double topBarStartX) {
            super(keybind, null, x, y, width, height);
            this.topBarStartX = topBarStartX;
        }

        // Renders L-shaped Enter key as 3 NON-overlapping quads to avoid transparency artifacts
        public void render(KeyboardHud hud, HudRenderer renderer, double s, SettingColor mutableColor) {
            double kX = hud.x + (x - hud.minX) * s;
            double kY = hud.y + (y - hud.minY) * s;
            double kW = width * s;
            double kH = height * s;
            double u = 35 * s;

            Color color = hud.getKeyColor(this, mutableColor);

            // Calculate positions
            double stemRight = kX + kW;
            double topBarX = hud.x + (topBarStartX - hud.minX) * s;
            double topBarLeftWidth = stemRight - topBarX - kW;
            if (topBarLeftWidth > 0) {
                renderer.quad(topBarX, kY, topBarLeftWidth, u, color);
            }
            renderer.quad(kX, kY, kW, u, color);
            renderer.quad(kX, kY + u, kW, kH - u, color);


            // Border
            if (hud.border.get()) {
                Color bColor = hud.getColor(hud.borderColor.get(), mutableColor);
                double bw = hud.borderWidth.get();
                double fullTopBarWidth = topBarLeftWidth + kW;  // Total width of top bar

                // Top bar borders
                renderer.quad(topBarX, kY, fullTopBarWidth, bw, bColor);  // Top edge
                renderer.quad(topBarX, kY, bw, u, bColor);  // Left edge
                renderer.quad(topBarX + fullTopBarWidth - bw, kY, bw, kH, bColor);  // Right edge (full height)

                // Stem borders
                renderer.quad(kX, kY + kH - bw, kW, bw, bColor);  // Bottom edge
                renderer.quad(kX, kY + u, bw, kH - u, bColor);  // Left edge (from u downward)

                // Bottom of top bar (connecting piece between top bar and stem)
                if (topBarLeftWidth > 0) {
                    renderer.quad(topBarX, kY + u - bw, topBarLeftWidth, bw, bColor);
                }
            }

            // Text centered in stem
            String text = getName();
            Color txtColor = hud.getColor(hud.textColor.get(), mutableColor);

            double padding = 2 * s;
            double availableWidth = kW - padding * 2;
            double availableHeight = kH - padding * 2;
            double tH = renderer.textHeight();
            double tW = renderer.textWidth(text);

            double widthScale = tW > availableWidth ? availableWidth / tW : 1.0;
            double heightScale = tH > availableHeight * 0.6 ? (availableHeight * 0.6) / tH : 1.0;
            double textScale = Math.min(widthScale, heightScale);

            double yText = kY + (kH - tH * textScale) / 2;
            hud.drawTextLine(renderer, text, tW, kX, yText, kW, textScale, txtColor);
        }
    }

    private static class RollingCps {
        private final LongList clicks = new LongArrayList();

        public void add() {
            clicks.add(System.currentTimeMillis());
        }

        public int get() {
            long time = System.currentTimeMillis();
            clicks.removeIf(val -> val + 1000 < time);
            return clicks.size();
        }
    }

    public class CustomKeyListSetting extends Setting<List<Key>> {
        public CustomKeyListSetting() {
            super("custom-keys", "Configure the custom keys display.", List.of(), k -> onPresetChanged(preset.get()), s -> {
            }, () -> preset.get() == Preset.Custom);
        }

        @Override
        protected void resetImpl() {
            this.value = new ObjectArrayList<>();
            this.value.add(new Key());
        }

        @Override
        protected List<Key> parseImpl(String str) {
            return List.of();
        }

        @Override
        protected boolean isValueValid(List<Key> value) {
            return true;
        }

        @Override
        protected NbtCompound save(NbtCompound tag) {
            NbtList valueTag = new NbtList();
            for (Key key : get()) {
                valueTag.add(key.serialize());
            }
            tag.put("value", valueTag);

            return tag;
        }

        @Override
        protected List<Key> load(NbtCompound tag) {
            get().clear();

            for (NbtElement tagI : tag.getListOrEmpty("value")) {
                tagI.asCompound().ifPresent(nbtCompound -> get().add(new Key(nbtCompound)));
            }

            return get();
        }
    }

    public static class CustomKeySettingScreen extends WindowScreen {
        private final CustomKeyListSetting setting;
        private final Key key;
        private final WidgetScreen screen;

        public CustomKeySettingScreen(GuiTheme theme, CustomKeyListSetting setting, Key key, WidgetScreen screen) {
            super(theme, "Select Key");
            this.setting = setting;
            this.key = key;
            this.screen = screen;
        }

        @Override
        public void initWidgets() {
            Settings settings = new Settings();
            SettingGroup sgGeneral = settings.getDefaultGroup();

            sgGeneral.add(new KeybindSetting.Builder()
                .name("custom-key")
                .description("The key to display.")
                .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_SPACE))
                .onChanged(k -> {
                    this.key.keybind = k;
                    this.screen.reload();
                })
                .onModuleActivated(setting -> setting.set(this.key.keybind))
                .build()
            );

            sgGeneral.add(new StringSetting.Builder()
                .name("custom-label")
                .description("Replace the Key name with custom text.")
                .defaultValue("")
                .onChanged(s -> this.key.name = s)
                .onModuleActivated(setting -> setting.set(this.key.name))
                .build()
            );

            sgGeneral.add(new DoubleSetting.Builder()
                .name("key-width")
                .description("Width of the key.")
                .defaultValue(60)
                .min(20)
                .sliderRange(20, 200)
                .decimalPlaces(1)
                .onChanged(d -> {
                    this.key.width = d;
                    this.setting.onChanged();
                })
                .onModuleActivated(setting -> setting.set(this.key.width))
                .build()
            );

            sgGeneral.add(new DoubleSetting.Builder()
                .name("key-height")
                .description("Height of the key.")
                .defaultValue(40)
                .min(20)
                .sliderRange(20, 200)
                .decimalPlaces(1)
                .onChanged(d -> {
                    this.key.height = d;
                    this.setting.onChanged();
                })
                .onModuleActivated(setting -> setting.set(this.key.height))
                .build()
            );

            sgGeneral.add(new DoubleSetting.Builder()
                .name("key-x")
                .description("X position offset of the key.")
                .defaultValue(0)
                .sliderRange(-200, 200)
                .decimalPlaces(1)
                .onChanged(d -> {
                    this.key.x = d;
                    this.setting.onChanged();
                })
                .onModuleActivated(setting -> setting.set(this.key.x))
                .build()
            );

            sgGeneral.add(new DoubleSetting.Builder()
                .name("key-y")
                .description("Y position offset of the key.")
                .defaultValue(0)
                .sliderRange(-200, 200)
                .decimalPlaces(1)
                .onChanged(d -> {
                    this.key.y = d;
                    this.setting.onChanged();
                })
                .onModuleActivated(setting -> setting.set(this.key.y))
                .build()
            );

            sgGeneral.add(new BoolSetting.Builder()
                .name("show-cps")
                .description("Show CPS for this key.")
                .defaultValue(false)
                .onChanged(b -> this.key.showCps = b)
                .onModuleActivated(setting -> setting.set(this.key.showCps))
                .build()
            );

            settings.onActivated();

            this.add(theme.settings(settings)).expandX();
        }
    }

    public static void fillTable(GuiTheme theme, WTable table, CustomKeyListSetting setting) {
        table.clear();

        for (Iterator<Key> it = setting.get().iterator(); it.hasNext(); ) {
            Key key = it.next();

            table.add(theme.label("Key")).expandWidgetX().widget().color(theme.textSecondaryColor());
            table.add(theme.label(String.format("(%s)", key.keybind))).expandWidgetX();

            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).expandCellX().widget();
            edit.action = () -> {
                WidgetScreen screen = (WidgetScreen) mc.currentScreen;
                mc.setScreen(new CustomKeySettingScreen(theme, setting, key, screen));
            };

            WMinus delete = table.add(theme.minus()).right().widget();
            delete.action = () -> {
                it.remove();
                setting.onChanged();

                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!setting.get().isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton add = table.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            Key newKey = new Key();
            // Position new key to the right of existing keys to avoid overlap
            if (!setting.get().isEmpty()) {
                Key lastKey = setting.get().getLast();
                newKey.x = lastKey.x + lastKey.width + 10;
                newKey.y = lastKey.y;
            }
            setting.get().add(newKey);
            setting.onChanged();

            fillTable(theme, table, setting);
        };

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();

            fillTable(theme, table, setting);
        };
        reset.tooltip = "Reset";
    }
}
