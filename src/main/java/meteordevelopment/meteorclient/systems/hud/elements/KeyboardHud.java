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
            if (binding != null) return binding.getBoundKeyLocalizedText().getString().toUpperCase();
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
