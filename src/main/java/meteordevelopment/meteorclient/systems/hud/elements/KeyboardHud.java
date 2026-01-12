/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

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
    public static final HudElementInfo<KeyboardHud> INFO = new HudElementInfo<>(Hud.GROUP, "keyboard",
            "Displays pressed keys.", KeyboardHud::new);

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

    private final Setting<Boolean> showCps = sgGeneral.add(new BoolSetting.Builder()
        .name("Show CPS")
        .description("Shows CPS on mouse buttons in Clicks preset.")
        .defaultValue(true)
        .visible(() -> preset.get() == Preset.Clicks)
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
        .defaultValue(0.2d)
        .min(0.01d)
        .sliderRange(0.01d, 1d)
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
        double w = 40;
        double h = 40;
        double s = spacing.get() * 2;

        switch (preset) {
            case Movement -> {
                keys.add(new Key(mc.options.forwardKey, w + s, 0, w, h));
                keys.add(new Key(mc.options.leftKey, 0, h + s, w, h));
                keys.add(new Key(mc.options.backKey, w + s, h + s, w, h));
                keys.add(new Key(mc.options.rightKey, (w + s) * 2, h + s, w, h));
                keys.add(new Key(mc.options.sneakKey, 0, (h + s) * 2, w, h));
                keys.add(new Key(mc.options.jumpKey, w + s, (h + s) * 2, (w * 2) + s, h));
            }
            case Clicks -> {
                keys.add(new Key(mc.options.attackKey, "LMB", 0, 0, w, h).setShowCps(showCps.get()));
                keys.add(new Key(mc.options.useKey, "RMB", w + s, 0, w, h).setShowCps(showCps.get()));
            }
            case Actions -> {
                keys.add(new Key(mc.options.dropKey, 0, 0, w, h));
                keys.add(new Key(mc.options.swapHandsKey, w + s, 0, w, h));
                keys.add(new Key(mc.options.inventoryKey, (w + s) * 2, 0, w, h));
            }
            case Hotbar -> {
                for (int i = 0; i < 9; i++) {
                    keys.add(new Key(mc.options.hotbarKeys[i], i * (w + s), 0, w, h));
                }
            }
            case Keyboard -> {
                double u = 35;  // base key unit size
                double g = s;   // gap between keys (spacing setting)
                double row0 = 0, row1 = u + 15 + g, row2 = row1 + u + g, row3 = row2 + u + g, row4 = row3 + u + g, row5 = row4 + u + g;

                double targetLen = 14 * (u + g) + u * 0.6;
                double padding;

                // Row 0: ESC, F1-F12, Print/Scroll/Pause
                padding = targetLen - (u * 13 + g * 12);
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_ESCAPE), 0, row0, u, u));
                for (int i = 0; i < 4; i++) keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F1 + i), (u + g) + padding / 2 + i * (u + g), row0, u, u));
                for (int i = 0; i < 4; i++) keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F5 + i), (u + g) * 5 + padding * 3 / 4 + i * (u + g), row0, u, u));
                for (int i = 0; i < 4; i++) keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F9 + i), (u + g) * 9 + padding + i * (u + g), row0, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PRINT_SCREEN), (u + g) * 15.5, row0, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_SCROLL_LOCK), (u + g) * 16.5, row0, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PAUSE), (u + g) * 17.5, row0, u, u));

                // Row 1: ` 1-0 - = BS, Ins/Home/PgUp
                int[] row1Keys = {GLFW.GLFW_KEY_GRAVE_ACCENT, GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0, GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_EQUAL};
                for (int i = 0; i < row1Keys.length; i++) keys.add(new Key(Keybind.fromKey(row1Keys[i]), i * (u + g), row1, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSPACE), 13 * (u + g), row1, u * 1.6 + g, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_INSERT), (u + g) * 15.5, row1, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_HOME), (u + g) * 16.5, row1, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_UP), (u + g) * 17.5, row1, u, u));

                // Row 2: Tab QWERTY..., Del/End/PgDn
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_TAB), 0, row2, u * 1.5, u));
                int[] row2Keys = {GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_U, GLFW.GLFW_KEY_I, GLFW.GLFW_KEY_O, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_LEFT_BRACKET, GLFW.GLFW_KEY_RIGHT_BRACKET};
                for (int i = 0; i < row2Keys.length; i++) keys.add(new Key(Keybind.fromKey(row2Keys[i]), u * 1.5 + g + i * (u + g), row2, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSLASH), u * 1.5 + g + 12 * (u + g), row2, u * 1.1 + g, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_DELETE), (u + g) * 15.5, row2, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_END), (u + g) * 16.5, row2, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_DOWN), (u + g) * 17.5, row2, u, u));

                // Row 3: Caps ASDF..., Enter
                padding = targetLen - (u * 11 + g * 12);
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_CAPS_LOCK), 0, row3, padding * (1.75 / 3.75), u));
                int[] row3Keys = {GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_H, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K, GLFW.GLFW_KEY_L, GLFW.GLFW_KEY_SEMICOLON, GLFW.GLFW_KEY_APOSTROPHE};
                for (int i = 0; i < row3Keys.length; i++) keys.add(new Key(Keybind.fromKey(row3Keys[i]), padding * (1.75 / 3.75) + g + i * (u + g), row3, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_ENTER), padding * (1.75 / 3.75) + g + 11 * (u + g), row3, padding * (2 / 3.75), u));

                // Row 4: LShift ZXCV..., RShift, Up
                padding = targetLen - (u * 10 + g * 11);
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SHIFT), 0, row4, padding * (2.1 / 4.8), u));
                int[] row4Keys = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_M, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_SLASH};
                for (int i = 0; i < row4Keys.length; i++) keys.add(new Key(Keybind.fromKey(row4Keys[i]), padding * (2.1 / 4.8) + g + i * (u + g), row4, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SHIFT), padding * (2.1 / 4.8) + g + 10 * (u + g), row4, padding * (2.7 / 4.8), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_UP), (u + g) * 16.5, row4, u, u));

                // Row 5: Ctrl/Win/Alt/Space/Alt/Win/Menu/Ctrl, Arrows
                padding = targetLen - (g * 7);
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_CONTROL), 0, row5, padding * (1.4 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SUPER), padding * (1.4 / 14.9) + g, row5, padding * (1.25 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_ALT), padding * (2.65 / 14.9) + g * 2, row5, padding * (1.25 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_SPACE), padding * (3.9 / 14.9) + g * 3, row5, padding * (5.6 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_ALT), padding * (9.5 / 14.9) + g * 4, row5, padding * (1.25 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SUPER), padding * (10.75 / 14.9) + g * 5, row5, padding * (1.25 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_MENU), padding * (12 / 14.9) + g * 6, row5, padding * (1.4 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_CONTROL), padding * (13.4 / 14.9) + g * 7, row5, padding * (1.5 / 14.9), u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT), (u + g) * 15.5, row5, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_DOWN), (u + g) * 16.5, row5, u, u));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT), (u + g) * 17.5, row5, u, u));
            }
            case Custom -> keys.addAll(customKeys.get());
        }
        calculateSize();
    }

    private void calculateSize() {
        if (keys.isEmpty()) {
            setSize(0, 0);
            return;
        }
        minX = minY = 0;
        double maxX = 0, maxY = 0;

        for (Key key : keys) {
            minX = Math.min(minX, key.x); minY = Math.min(minY, key.y);
            maxX = Math.max(maxX, key.x + key.width); maxY = Math.max(maxY, key.y + key.height);
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
            double availableWidth = kW - padding * 2;
            double availableHeight = kH - padding * 2;
            double tH = renderer.textHeight();

            if (!key.showCps) {
                double tW = renderer.textWidth(text);
                double widthScale = tW > availableWidth ? availableWidth / tW : 1.0;
                double heightScale = tH > availableHeight * 0.6 ? (availableHeight * 0.6) / tH : 1.0;
                double textScale = Math.min(widthScale, heightScale);
                double yText = kY + (kH - tH * textScale) / 2;
                drawTextLine(renderer, text, tW, kX, yText, kW, textScale, txtColor);
            } else {
                double topW = renderer.textWidth(text);
                double topWidthScale = topW > availableWidth ? availableWidth / topW : 1.0;
                double topHeightScale = tH > availableHeight * 0.4 ? (availableHeight * 0.4) / tH : 1.0;
                double topScale = Math.min(topWidthScale, topHeightScale);

                String cpsText = key.getCps() + " CPS";
                double botW = renderer.textWidth(cpsText);
                double botWidthScale = botW > availableWidth ? availableWidth / botW : 1.0;
                double botHeightScale = tH > availableHeight * 0.4 ? (availableHeight * 0.4) / tH : 1.0;
                double botScale = Math.min(botWidthScale, botHeightScale);

                double totalHeight = (tH * topScale) + (tH * botScale);
                double startY = kY + (kH - totalHeight) / 2;

                drawTextLine(renderer, text, topW, kX, startY, kW, topScale, txtColor);
                drawTextLine(renderer, cpsText, botW, kX, startY + tH * topScale, kW, botScale, txtColor);
            }
        }
    }

    private void drawTextLine(HudRenderer renderer, String text, double textWidth, double x, double y, double w, double textScale,
            Color color) {
        double s = scale.get();
        double padding = 2 * s;

        double xText = x + (w - textWidth * textScale) / 2;
        if (alignment.get() == Alignment.Left)
            xText = x + padding;
        else if (alignment.get() == Alignment.Right)
            xText = x + w - padding - textWidth * textScale;

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

    public static class Key {
        public String name = "";
        public KeyBinding binding;
        public Keybind keybind;
        public int code;
        public double x, y, width, height;
        public boolean showCps = false;

        private final RollingCps rollingCps = new RollingCps();
        private boolean isPressed;
        private float delta;

        public Key() {
            this(Keybind.fromKey(GLFW.GLFW_KEY_SPACE), 0, 0, 60, 40);
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

        public Key(KeyBinding binding, double x, double y, double width, double height) {
            this(binding, null, x, y, width, height);
        }

        public Key(KeyBinding binding, String name, double x, double y, double width, double height) {
            this.binding = binding;
            this.name = name;
            this.code = -1;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Key(Keybind keybind, double x, double y, double width, double height) {
            this.keybind = keybind;
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
            if (keybind != null ) {
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
            if (action == KeyAction.Press) {
                isPressed = true;
                if (showCps) {
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
            super("custom-keys", "Configure the custom keys display.", List.of(), k -> onPresetChanged(preset.get()), s -> {}, () -> preset.get() == Preset.Custom);
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

        for (Iterator<Key> it = setting.get().iterator(); it.hasNext();) {
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
            setting.get().add(new Key());
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
