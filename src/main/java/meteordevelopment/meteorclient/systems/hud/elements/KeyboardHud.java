/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.mixin.KeyBindingAccessor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

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

    private final Setting<String> customKeys = sgGeneral.add(new StringSetting.Builder()
        .name("custom-keys")
        .description("Custom keys configuration (KeyName X Y Width Height [flags]...). Flags: cps")
        .defaultValue("LMB 0 0 40 40 cps, RMB 44 0 40 40 cps")
        .visible(() -> preset.get() == Preset.AdvancedCustomization)
        .onChanged(this::onCustomKeysChanged)
        .build()
    );

    private final Setting<Keybind> customKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("custom-key")
        .description("The key to display.")
        .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_SPACE))
        .visible(() -> preset.get() == Preset.CustomKey)
        .onChanged(k -> onPresetChanged(preset.get()))
        .build()
    );

    private final Setting<Double> customKeyWidth = sgGeneral.add(new DoubleSetting.Builder()
        .name("key-width")
        .description("Width of the key.")
        .defaultValue(60)
        .min(20)
        .sliderRange(20, 200)
        .visible(() -> preset.get() == Preset.CustomKey)
        .onChanged(v -> onPresetChanged(preset.get()))
        .build()
    );

    private final Setting<Double> customKeyHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("key-height")
        .description("Height of the key.")
        .defaultValue(40)
        .min(20)
        .sliderRange(20, 200)
        .visible(() -> preset.get() == Preset.CustomKey)
        .onChanged(v -> onPresetChanged(preset.get()))
        .build()
    );

    private final Setting<Boolean> customKeyShowCps = sgGeneral.add(new BoolSetting.Builder()
        .name("show-cps")
        .description("Show CPS for this key.")
        .defaultValue(false)
        .visible(() -> preset.get() == Preset.CustomKey)
        .onChanged(v -> onPresetChanged(preset.get()))
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the keyboard.")
        .defaultValue(1.5)
        .min(0.5)
        .sliderRange(0.5, 5)
        .onChanged(s -> calculateSize())
        .build()
    );

    private final Setting<Double> spacing = sgGeneral.add(new DoubleSetting.Builder()
        .name("spacing")
        .description("Spacing between keys.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 10)
        .visible(() -> preset.get() != Preset.CustomKey && preset.get() != Preset.AdvancedCustomization)
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
    public WWidget getWidget(GuiTheme theme) {
        return null;
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
            case CustomKey -> {
                Key key = new Key(customKey.get(), 0, 0, customKeyWidth.get(), customKeyHeight.get());
                key.setShowCps(customKeyShowCps.get());
                keys.add(key);
            }
            case AdvancedCustomization -> {
                onCustomKeysChanged(customKeys.get());
                ChatUtils.info("Keyboard HUD Custom Format: Key X Y Weight Height [cps]. Ex: 'LMB 0 0 40 40 cps'.");
            }
        }
        calculateSize();
    }

    private void onCustomKeysChanged(String config) {
        if (preset.get() != Preset.AdvancedCustomization)
            return;
        keys.clear();

        String[] parts = config.split(",");
        for (String part : parts) {
            String[] args = part.trim().split(" ");
            if (args.length >= 3) {
                try {
                    String name = args[0];
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double w = args.length > 3 ? Double.parseDouble(args[3]) : 40;
                    double h = args.length > 4 ? Double.parseDouble(args[4]) : 40;

                    int code = resolveKey(name);
                    Key key = new Key(name, code, x, y, w, h);

                    if (args.length > 5) {
                        for (int i = 5; i < args.length; i++) {
                            if (args[i].equalsIgnoreCase("cps")) {
                                key.setShowCps(true);
                            }
                        }
                    }

                    keys.add(key);
                } catch (Exception ignored) {
                }
            }
        }
        calculateSize();
    }

    private int resolveKey(String name) {
        if (name.length() == 1) {
            int c = name.toUpperCase().charAt(0);
            if (c >= 'A' && c <= 'Z')
                return GLFW.GLFW_KEY_A + (c - 'A');
            if (c >= '0' && c <= '9')
                return GLFW.GLFW_KEY_0 + (c - '0');
            return switch (name) {
                case "[" -> GLFW.GLFW_KEY_LEFT_BRACKET;
                case "]" -> GLFW.GLFW_KEY_RIGHT_BRACKET;
                case ";" -> GLFW.GLFW_KEY_SEMICOLON;
                case "'" -> GLFW.GLFW_KEY_APOSTROPHE;
                case "\\" -> GLFW.GLFW_KEY_BACKSLASH;
                case "`" -> GLFW.GLFW_KEY_GRAVE_ACCENT;
                case "-" -> GLFW.GLFW_KEY_MINUS;
                case "=" -> GLFW.GLFW_KEY_EQUAL;
                case "." -> GLFW.GLFW_KEY_PERIOD;
                case "/" -> GLFW.GLFW_KEY_SLASH;
                default -> -1;
            };
        }
        return switch (name.toUpperCase()) {
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            case "LSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "CTRL", "LCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "ALT", "LALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "ESC" -> GLFW.GLFW_KEY_ESCAPE;
            case "CAPS" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "WIN", "SUPER", "LWIN", "LSUPER" -> GLFW.GLFW_KEY_LEFT_SUPER;
            case "RWIN", "RSUPER" -> GLFW.GLFW_KEY_RIGHT_SUPER;
            case "PRINT", "PRTSC" -> GLFW.GLFW_KEY_PRINT_SCREEN;
            case "SCROLL" -> GLFW.GLFW_KEY_SCROLL_LOCK;
            case "PAUSE" -> GLFW.GLFW_KEY_PAUSE;
            case "NUMLOCK" -> GLFW.GLFW_KEY_NUM_LOCK;
            case "COMMA" -> GLFW.GLFW_KEY_COMMA;
            case "PERIOD", "DOT" -> GLFW.GLFW_KEY_PERIOD;
            case "SLASH" -> GLFW.GLFW_KEY_SLASH;
            case "F1" -> GLFW.GLFW_KEY_F1;
            case "F2" -> GLFW.GLFW_KEY_F2;
            case "F3" -> GLFW.GLFW_KEY_F3;
            case "F4" -> GLFW.GLFW_KEY_F4;
            case "F5" -> GLFW.GLFW_KEY_F5;
            case "F6" -> GLFW.GLFW_KEY_F6;
            case "F7" -> GLFW.GLFW_KEY_F7;
            case "F8" -> GLFW.GLFW_KEY_F8;
            case "F9" -> GLFW.GLFW_KEY_F9;
            case "F10" -> GLFW.GLFW_KEY_F10;
            case "F11" -> GLFW.GLFW_KEY_F11;
            case "F12" -> GLFW.GLFW_KEY_F12;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            case "HOME" -> GLFW.GLFW_KEY_HOME;
            case "END" -> GLFW.GLFW_KEY_END;
            case "PAGEUP", "PGUP" -> GLFW.GLFW_KEY_PAGE_UP;
            case "PAGEDOWN", "PGDN" -> GLFW.GLFW_KEY_PAGE_DOWN;
            case "INSERT", "INS" -> GLFW.GLFW_KEY_INSERT;
            case "DELETE", "DEL" -> GLFW.GLFW_KEY_DELETE;
            case "NUM0" -> GLFW.GLFW_KEY_KP_0;
            case "NUM1" -> GLFW.GLFW_KEY_KP_1;
            case "NUM2" -> GLFW.GLFW_KEY_KP_2;
            case "NUM3" -> GLFW.GLFW_KEY_KP_3;
            case "NUM4" -> GLFW.GLFW_KEY_KP_4;
            case "NUM5" -> GLFW.GLFW_KEY_KP_5;
            case "NUM6" -> GLFW.GLFW_KEY_KP_6;
            case "NUM7" -> GLFW.GLFW_KEY_KP_7;
            case "NUM8" -> GLFW.GLFW_KEY_KP_8;
            case "NUM9" -> GLFW.GLFW_KEY_KP_9;
            case "NUMADD" -> GLFW.GLFW_KEY_KP_ADD;
            case "NUMSUB" -> GLFW.GLFW_KEY_KP_SUBTRACT;
            case "NUMMUL" -> GLFW.GLFW_KEY_KP_MULTIPLY;
            case "NUMDIV" -> GLFW.GLFW_KEY_KP_DIVIDE;
            case "NUMDOT" -> GLFW.GLFW_KEY_KP_DECIMAL;
            case "NUMENTER" -> GLFW.GLFW_KEY_KP_ENTER;
            case "LMB", "MB1" -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
            case "RMB", "MB2" -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case "MMB", "MB3" -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            case "MB4" -> GLFW.GLFW_MOUSE_BUTTON_4;
            case "MB5" -> GLFW.GLFW_MOUSE_BUTTON_5;
            case "MB6" -> GLFW.GLFW_MOUSE_BUTTON_6;
            case "MB7" -> GLFW.GLFW_MOUSE_BUTTON_7;
            case "MB8" -> GLFW.GLFW_MOUSE_BUTTON_8;
            default -> -1;
        };
    }

    private void calculateSize() {
        if (keys.isEmpty()) {
            setSize(0, 0);
            return;
        }

        double maxX = 0;
        double maxY = 0;

        for (Key key : keys) {
            maxX = Math.max(maxX, key.x + key.width);
            maxY = Math.max(maxY, key.y + key.height);
        }

        double s = scale.get();
        setSize(maxX * s, maxY * s);
    }

    private String getShortName(String name) {
        if (name == null)
            return "?";
        return switch (name.toUpperCase()) {
            case "LEFT SHIFT", "LSHIFT" -> "LSh";
            case "RIGHT SHIFT", "RSHIFT" -> "RSh";
            case "LEFT CONTROL", "LCTRL" -> "LCtrl";
            case "RIGHT CONTROL", "RCTRL" -> "RCtrl";
            case "LEFT ALT", "LALT" -> "LAlt";
            case "RIGHT ALT", "RALT" -> "RAlt";
            case "COMMA" -> ",";
            case "DOT", "PERIOD" -> ".";
            case "SLASH" -> "/";
            case "BACKSPACE" -> "BS";
            case "ENTER" -> "Ent";
            case "SCROLL" -> "ScrL";
            case "PRINT", "PRTSC" -> "PrtS";
            case "PAUSE" -> "Paus";
            case "PAGEUP", "PGUP" -> "PgUp";
            case "PAGEDOWN", "PGDN" -> "PgDn";
            case "INSERT", "INS" -> "Ins";
            case "DELETE", "DEL" -> "Del";
            case "HOME" -> "Home";
            case "END" -> "End";
            case "UP" -> "Up";
            case "DOWN" -> "Dn";
            case "LEFT" -> "Lt";
            case "RIGHT" -> "Rt";
            default -> name;
        };
    }

    @Override
    public void render(HudRenderer renderer) {
        if (keys.isEmpty()) {
            if (mc.options != null && preset.get() != Preset.AdvancedCustomization) {
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

            double kX = x + key.x * s;
            double kY = y + key.y * s;
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
        CustomKey,
        AdvancedCustomization
    }

    private static class Key {
        public String name;
        public KeyBinding binding;
        public Keybind keybind;
        public int code;
        public double x, y, width, height;
        public boolean showCps = false;

        private final RollingCps rollingCps = new RollingCps();
        private boolean wasPressed;

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

        public Key(String name, int code, double x, double y, double width, double height) {
            this.name = name;
            this.code = code;
            this.binding = null;
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
            if (keybind != null) return keybind.toString();
            if (name != null) return name;
            if (binding != null) return binding.getBoundKeyLocalizedText().getString().toUpperCase();
            return "?";
        }

        public boolean isPressed() {
            long window = mc.getWindow().getHandle();
            if (keybind != null) {
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
}
