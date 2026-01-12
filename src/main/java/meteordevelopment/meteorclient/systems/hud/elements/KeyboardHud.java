/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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

    private final Setting<SettingColor> textColor = sgColor.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the key name.")
        .defaultValue(new SettingColor(255, 255, 255))
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

    private Color getColor(SettingColor color) {
        Color c = new Color(color);
        c.a = (int) (c.a * opacity.get());
        return c;
    }

    public KeyboardHud() {
        super(INFO);
        if (mc.options != null)
            onPresetChanged(preset.get());
    }

    @Override
    public void tick(HudRenderer renderer) {
        for (Key key : keys) {
            key.tick();
        }
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
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_ESCAPE), 0, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F1), 70, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F2), 109, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F3), 148, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F4), 187, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F5), 241, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F6), 280, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F7), 319, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F8), 358, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F9), 412, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F10), 451, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F11), 490, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F12), 529, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PRINT_SCREEN), 578, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_SCROLL_LOCK), 617, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PAUSE), 656, 0, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_GRAVE_ACCENT), 0, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_1), 39, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_2), 78, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_3), 117, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_4), 156, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_5), 195, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_6), 234, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_7), 273, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_8), 312, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_9), 351, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_0), 390, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_MINUS), 429, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_EQUAL), 468, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSPACE), 507, 50, 57, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_INSERT), 578, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_HOME), 617, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_UP), 656, 50, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_TAB), 0, 89, 52, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_Q), 56, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_W), 95, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_E), 134, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_R), 173, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_T), 212, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_Y), 251, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_U), 290, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_I), 329, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_O), 368, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_P), 407, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_BRACKET), 446, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_BRACKET), 485, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_BACKSLASH), 524, 89, 40, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_DELETE), 578, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_END), 617, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PAGE_DOWN), 656, 89, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_CAPS_LOCK), 0, 128, 62, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_A), 66, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_S), 105, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_D), 144, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_F), 183, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_G), 222, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_H), 261, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_J), 300, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_K), 339, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_L), 378, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_SEMICOLON), 417, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_APOSTROPHE), 456, 128, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_ENTER), 495, 128, 69, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SHIFT), 0, 167, 75, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_Z), 79, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_X), 118, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_C), 157, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_V), 196, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_B), 235, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_N), 274, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_M), 313, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_COMMA), 352, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_PERIOD), 391, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_SLASH), 430, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SHIFT), 469, 167, 95, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_UP), 617, 167, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_CONTROL), 0, 206, 50, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_SUPER), 54, 206, 45, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT_ALT), 103, 206, 45, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_SPACE), 152, 206, 200, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_ALT), 356, 206, 45, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_SUPER), 405, 206, 45, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_MENU), 454, 206, 50, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT_CONTROL), 508, 206, 56, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_LEFT), 578, 206, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_DOWN), 617, 206, 35, 35));
                keys.add(new Key(Keybind.fromKey(GLFW.GLFW_KEY_RIGHT), 656, 206, 35, 35));
            }
            case Custom -> {
                keys.addAll(customKeys.get());
            }
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

    private String getShortName(String name) {
        if (name == null)
            return "?";
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
            case "SCROLL" -> "ScrL";
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

        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), getColor(backgroundColor.get()));
        }

        double s = scale.get();

        for (Key key : keys) {
            boolean pressed = key.isPressed();
            Color color = pressed ? getColor(pressedColor.get()) : getColor(unpressedColor.get());

            double kX = x + (key.x - minX) * s;
            double kY = y + (key.y - minY) * s;
            double kW = key.width * s;
            double kH = key.height * s;

            renderer.quad(kX, kY, kW, kH, color);

            String text = getShortName(key.getName());
            String cpsText = key.showCps ? key.getCps() + " CPS" : "";
            Color txtColor = getColor(textColor.get());

            double padding = 2 * s;
            double availableWidth = kW - padding * 2;
            double availableHeight = kH - padding * 2;
            double tH = renderer.textHeight();

            if (cpsText.isEmpty()) {
                double tW = renderer.textWidth(text);
                double widthScale = tW > availableWidth ? availableWidth / tW : 1.0;
                double heightScale = tH > availableHeight * 0.6 ? (availableHeight * 0.6) / tH : 1.0;
                double textScale = Math.min(widthScale, heightScale);
                double yText = kY + (kH - tH * textScale) / 2;
                drawTextLine(renderer, text, kX, yText, kW, textScale, txtColor);
            } else {
                double topW = renderer.textWidth(text);
                double topWidthScale = topW > availableWidth ? availableWidth / topW : 1.0;
                double topHeightScale = tH > availableHeight * 0.4 ? (availableHeight * 0.4) / tH : 1.0;
                double topScale = Math.min(topWidthScale, topHeightScale);

                double botW = renderer.textWidth(cpsText);
                double botWidthScale = botW > availableWidth ? availableWidth / botW : 1.0;
                double botHeightScale = tH > availableHeight * 0.4 ? (availableHeight * 0.4) / tH : 1.0;
                double botScale = Math.min(botWidthScale, botHeightScale);

                double totalHeight = (tH * topScale) + (tH * botScale);
                double startY = kY + (kH - totalHeight) / 2;

                drawTextLine(renderer, text, kX, startY, kW, topScale, txtColor);
                drawTextLine(renderer, cpsText, kX, startY + tH * topScale, kW, botScale, txtColor);
            }
        }
    }

    private void drawTextLine(HudRenderer renderer, String text, double x, double y, double w, double textScale,
            Color color) {
        double tW = renderer.textWidth(text);
        double s = scale.get();
        double padding = 2 * s;

        double xText = x + (w - tW * textScale) / 2;
        if (alignment.get() == Alignment.Left)
            xText = x + padding;
        else if (alignment.get() == Alignment.Right)
            xText = x + w - padding - tW * textScale;

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
        private boolean wasPressed;

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
            if (keybind != null) return keybind.toString();
            if (binding != null) return binding.getBoundKeyLocalizedText().getString();
            return "?";
        }

        public boolean isPressed() {
            long window = mc.getWindow().getHandle();
            if (keybind != null) {
                if (!keybind.isSet()) return false;
                int key = keybind.getValue();
                boolean isMouse = !keybind.isKey();
                if (isMouse) return GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
                return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            }
            if (binding != null) {
                int key = ((KeyBindingAccessor) binding).meteor$getKey().getCode();
                if (key >= 0 && key < 8) {
                    return GLFW.glfwGetMouseButton(window, key) == GLFW.GLFW_PRESS;
                }
                return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            }
            if (code >= 0) {
                if (code < 8) return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
                return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
            }
            return false;
        }

        public void tick() {
            boolean pressed = isPressed();
            if (pressed && !wasPressed && showCps) {
                rollingCps.add();
            }
            wasPressed = pressed;
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
        private final List<Long> clicks = new ArrayList<>();

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
