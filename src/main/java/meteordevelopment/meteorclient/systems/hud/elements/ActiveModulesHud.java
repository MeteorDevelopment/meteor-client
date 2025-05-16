/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.ArrayList;
import java.util.List;

public class ActiveModulesHud extends HudElement {
    public static final HudElementInfo<ActiveModulesHud> INFO = new HudElementInfo<>(Hud.GROUP, "active-modules", "Displays your active modules.", ActiveModulesHud::new);

    private static final Color WHITE = new Color();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColor = settings.createGroup("Color");
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    private final Setting<Sort> sort = sgGeneral.add(new EnumSetting.Builder<Sort>()
        .name("sort")
        .description("How to sort active modules.")
        .defaultValue(Sort.Biggest)
        .build()
    );

    private final Setting<List<Module>> hiddenModules = sgGeneral.add(new ModuleListSetting.Builder()
        .name("hidden-modules")
        .description("Which modules not to show in the list.")
        .build()
    );

    private final Setting<Boolean> activeInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("module-info")
        .description("Shows info from the module next to the name in the active modules list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showKeybind = sgGeneral.add(new BoolSetting.Builder()
        .name("show-keybind")
        .description("Shows the module's keybind next to its name.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> outlines = sgGeneral.add(new BoolSetting.Builder()
        .name("outlines")
        .description("Whether or not to render outlines")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> outlineWidth = sgGeneral.add(new IntSetting.Builder()
        .name("outline-width")
        .description("Outline width")
        .defaultValue(2)
        .min(1)
        .sliderMin(1)
        .visible(outlines::get)
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal alignment.")
        .defaultValue(Alignment.Auto)
        .build()
    );

    // Color

    private final Setting<ColorMode> colorMode = sgColor.add(new EnumSetting.Builder<ColorMode>()
        .name("color-mode")
        .description("What color to use for active modules.")
        .defaultValue(ColorMode.Rainbow)
        .build()
    );

    private final Setting<SettingColor> flatColor = sgColor.add(new ColorSetting.Builder()
        .name("flat-color")
        .description("Color for flat color mode.")
        .defaultValue(new SettingColor(225, 25, 25))
        .visible(() -> colorMode.get() == ColorMode.Flat)
        .build()
    );

    private final Setting<Double> rainbowSpeed = sgColor.add(new DoubleSetting.Builder()
        .name("rainbow-speed")
        .description("Rainbow speed of rainbow color mode.")
        .defaultValue(0.05)
        .sliderMin(0.01)
        .sliderMax(0.2)
        .decimalPlaces(4)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<Double> rainbowSpread = sgColor.add(new DoubleSetting.Builder()
        .name("rainbow-spread")
        .description("Rainbow spread of rainbow color mode.")
        .defaultValue(0.01)
        .sliderMin(0.001)
        .sliderMax(0.05)
        .decimalPlaces(4)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<Double> rainbowSaturation = sgColor.add(new DoubleSetting.Builder()
        .name("rainbow-saturation")
        .defaultValue(1.0d)
        .sliderRange(0.0d, 1.0d)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<Double> rainbowBrightness = sgColor.add(new DoubleSetting.Builder()
        .name("rainbow-brightness")
        .defaultValue(1.0d)
        .sliderRange(0.0d, 1.0d)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<SettingColor> moduleInfoColor = sgColor.add(new ColorSetting.Builder()
        .name("module-info-color")
        .description("Color of module info text.")
        .defaultValue(new SettingColor(175, 175, 175))
        .visible(activeInfo::get)
        .build()
    );

    // Scale

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .min(0.5)
        .sliderRange(0.5, 3)
        .build()
    );

    // Background

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

    private final List<Module> modules = new ArrayList<>();

    private final Color rainbow = new Color(255, 255, 255);
    private double rainbowHue1;
    private double rainbowHue2;

    private double lastX;
    private double emptySpace;
    private double prevTextLength;
    private Color prevColor = new Color();

    public ActiveModulesHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        modules.clear();

        for (Module module : Modules.get().getActive()) {
            if (!hiddenModules.get().contains(module)) modules.add(module);
        }

        if (modules.isEmpty()) {
            if (isInEditor()) {
                setSize(renderer.textWidth("Active Modules", shadow.get(), getScale()), renderer.textHeight(shadow.get(), getScale()));
            }
            return;
        }

        modules.sort((e1, e2) -> switch (sort.get()) {
            case Alphabetical -> e1.title.compareTo(e2.title);
            case Biggest -> Double.compare(getModuleWidth(renderer, e2), getModuleWidth(renderer, e1));
            case Smallest -> Double.compare(getModuleWidth(renderer, e1), getModuleWidth(renderer, e2));
        });

        double width = 0;
        double height = 0;

        for (Module module : modules) {
            width = Math.max(width, getModuleWidth(renderer, module));
            height += renderer.textHeight(shadow.get(), getScale());
        }

        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = this.x;
        double y = this.y;

        if (modules.isEmpty()) {
            if (isInEditor()) {
                renderer.text("Active Modules", x, y, WHITE, shadow.get(), getScale());
            }
            return;
        }

        rainbowHue1 += rainbowSpeed.get() * renderer.delta;
        if (rainbowHue1 > 1) rainbowHue1 -= 1;
        else if (rainbowHue1 < -1) rainbowHue1 += 1;

        rainbowHue2 = rainbowHue1;

        lastX = x;
        emptySpace = renderer.textWidth(" ", shadow.get(), getScale());

        for (int i = 0; i < modules.size(); i++) {
            double offset = alignX(getModuleWidth(renderer, modules.get(i)), alignment.get());
            renderModule(renderer, i, x + offset, y);

            lastX = x + offset;
            y += renderer.textHeight(shadow.get(), getScale());
        }
    }

    private void renderModule(HudRenderer renderer, int index, double x, double y) {
        Module module = modules.get(index);
        Color color = flatColor.get();

        switch (colorMode.get()) {
            case Random -> color = module.color;
            case Rainbow -> {
                rainbowHue2 += rainbowSpread.get();
                int c = java.awt.Color.HSBtoRGB((float) rainbowHue2, rainbowSaturation.get().floatValue(), rainbowBrightness.get().floatValue());
                rainbow.r = Color.toRGBAR(c);
                rainbow.g = Color.toRGBAG(c);
                rainbow.b = Color.toRGBAB(c);
                color = rainbow;
            }
        }

        renderer.text(module.title, x, y, color, shadow.get(), getScale());

        double textHeight = renderer.textHeight(shadow.get(), getScale());
        double textLength = renderer.textWidth(module.title, shadow.get(), getScale());

        if (showKeybind.get() && module.keybind.isSet()) {
            String keybindStr = " [" + module.keybind + "]";
            renderer.text(keybindStr, x + textLength, y, moduleInfoColor.get(), shadow.get(), getScale());
            textLength += renderer.textWidth(keybindStr, shadow.get(), getScale());
        }

        if (activeInfo.get()) {
            String info = module.getInfoString();
            if (info != null) {
                renderer.text(info, x + textLength + emptySpace, y, moduleInfoColor.get(), shadow.get(), getScale());
                textLength += emptySpace + renderer.textWidth(info, shadow.get(), getScale());
            }
        }

        double lineStartY = y;
        double lineHeight = textHeight;

        if (outlines.get()) {
            if (index == 0) { // Render top quad for first item in list
                lineStartY -= 2;
                lineHeight += 2;

                renderer.quad(x - 2 - outlineWidth.get(), lineStartY - outlineWidth.get(),
                    textLength + 4 + 2 * outlineWidth.get(),
                    outlineWidth.get(), prevColor, prevColor, color, color);
            } else { // In-between quads are rendered above the current line so don't need for the top
                renderer.quad(Math.min(lastX, x) - 2 - outlineWidth.get(), Math.max(lastX, x) == x ? y : y - outlineWidth.get(),
                    (Math.max(lastX, x) - 2) - (Math.min(lastX, x) - 2 - outlineWidth.get()), outlineWidth.get(),
                    prevColor, prevColor, color, color); // Left in-between quad

                renderer.quad(Math.min(lastX + prevTextLength, x + textLength) + 2, Math.min(lastX + prevTextLength, x + textLength) == x + textLength ? y : y - outlineWidth.get(),
                    (Math.max(lastX + prevTextLength, x + textLength) + 2 + outlineWidth.get()) - (Math.min(lastX + prevTextLength, x + textLength) + 2), outlineWidth.get(),
                    prevColor, prevColor, color, color); // Right in-between quad
            }

            if (index == modules.size() - 1) { // Render bottom quad for last item in list
                lineHeight += 2;

                renderer.quad(x - 2 - outlineWidth.get(), lineStartY + lineHeight,
                    textLength + 4 + 2 * outlineWidth.get(), outlineWidth.get(),
                    prevColor, prevColor, color, color);
            }

            // Left side quad
            renderer.quad(x - 2 - outlineWidth.get(), lineStartY, outlineWidth.get(), lineHeight,
                prevColor, prevColor, color, color);

            // Right side quad
            renderer.quad(x + textLength + 2, lineStartY, outlineWidth.get(), lineHeight,
                prevColor, prevColor, color, color);
        }

        if (background.get()) {
            renderer.quad( x - 2, lineStartY, textLength + 4, lineHeight, backgroundColor.get());
        }

        prevTextLength = textLength;
        prevColor = color;
    }

    private double getModuleWidth(HudRenderer renderer, Module module) {
        double width = renderer.textWidth(module.title, shadow.get(), getScale());

        if (showKeybind.get() && module.keybind.isSet()) {
            width += renderer.textWidth(" [" + module.keybind + "]", shadow.get(), getScale());
        }

        if (activeInfo.get()) {
            String info = module.getInfoString();
            if (info != null) width += renderer.textWidth(" ", shadow.get(), getScale()) + renderer.textWidth(info, shadow.get(), getScale());
        }

        return width;
    }

    private double getScale() {
        return customScale.get() ? scale.get() : Hud.get().getTextScale();
    }

    public enum Sort {
        Alphabetical,
        Biggest,
        Smallest
    }

    public enum ColorMode {
        Flat,
        Random,
        Rainbow
    }

    public enum Background {
        None,
        Block,
        Text
    }
}
