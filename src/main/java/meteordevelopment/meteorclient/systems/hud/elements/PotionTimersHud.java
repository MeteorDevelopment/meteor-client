/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.*;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PotionTimersHud extends HudElement {
    public static final HudElementInfo<PotionTimersHud> INFO = new HudElementInfo<>(Hud.GROUP, "potion-timers", "Displays active potion effects with timers.", PotionTimersHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<List<StatusEffect>> hiddenEffects = sgGeneral.add(new StatusEffectListSetting.Builder()
        .name("hidden-effects")
        .description("Which effects not to show in the list.")
        .build()
    );

    private final Setting<Boolean> showAmbient = sgGeneral.add(new BoolSetting.Builder()
        .name("show-ambient")
        .description("Whether to show ambient effects like from beacons and conduits.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ColorMode> colorMode = sgGeneral.add(new EnumSetting.Builder<ColorMode>()
        .name("color-mode")
        .description("What color to use for effects.")
        .defaultValue(ColorMode.Effect)
        .build()
    );

    private final Setting<SettingColor> flatColor = sgGeneral.add(new ColorSetting.Builder()
        .name("flat-color")
        .description("Color for flat color mode.")
        .defaultValue(new SettingColor(225, 25, 25))
        .visible(() -> colorMode.get() == ColorMode.Flat)
        .build()
    );

    private final Setting<Double> rainbowSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rainbow-speed")
        .description("Rainbow speed of rainbow color mode.")
        .defaultValue(0.05)
        .sliderMin(0.01)
        .sliderMax(0.2)
        .decimalPlaces(4)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<Double> rainbowSpread = sgGeneral.add(new DoubleSetting.Builder()
        .name("rainbow-spread")
        .description("Rainbow spread of rainbow color mode.")
        .defaultValue(0.01)
        .sliderMin(0.001)
        .sliderMax(0.05)
        .decimalPlaces(4)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<Double> rainbowSaturation = sgGeneral.add(new DoubleSetting.Builder()
        .name("rainbow-saturation")
        .description("Saturation of rainbow color mode.")
        .defaultValue(1.0d)
        .sliderRange(0.0d, 1.0d)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<Double> rainbowBrightness = sgGeneral.add(new DoubleSetting.Builder()
        .name("rainbow-brightness")
        .description("Brightness of rainbow color mode.")
        .defaultValue(1.0d)
        .sliderRange(0.0d, 1.0d)
        .visible(() -> colorMode.get() == ColorMode.Rainbow)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders shadow behind text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
        .name("alignment")
        .description("Horizontal alignment.")
        .defaultValue(Alignment.Auto)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .build()
    );

    // Scale

    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies custom text scale rather than the global one.")
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

    private final List<Pair<StatusEffectInstance, String>> texts = new ArrayList<>();
    private double rainbowHue;

    public PotionTimersHud() {
        super(INFO);
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    @Override
    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth() - border.get() * 2, width, alignment);
    }

    @Override
    public void tick(HudRenderer renderer) {
        if (mc.player == null || (isInEditor() && hasNoVisibleEffects())) {
            setSize(renderer.textWidth("Potion Timers 0:00", shadow.get(), getScale()), renderer.textHeight(shadow.get(), getScale()));
            return;
        }

        double width = 0;
        double height = 0;

        texts.clear();

        for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
            if (hiddenEffects.get().contains(statusEffectInstance.getEffectType().value())) continue;
            if (!showAmbient.get() && statusEffectInstance.isAmbient()) continue;
            String text = getString(statusEffectInstance);
            texts.add(new ObjectObjectImmutablePair<>(statusEffectInstance, text));
            width = Math.max(width, renderer.textWidth(text, shadow.get(), getScale()));
            height += renderer.textHeight(shadow.get(), getScale());
        }

        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = this.x + border.get();
        double y = this.y + border.get();

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        if (mc.player == null || (isInEditor() && hasNoVisibleEffects())) {
            renderer.text("Potion Timers 0:00", x, y, Color.WHITE, shadow.get(), getScale());
            return;
        }

        rainbowHue += rainbowSpeed.get() * renderer.delta;
        if (rainbowHue > 1) rainbowHue -= 1;
        else if (rainbowHue < -1) rainbowHue += 1;

        double localRainbowHue = rainbowHue;

        for (Pair<StatusEffectInstance, String> potionEffectEntry : texts) {
            Color color = switch (colorMode.get()) {
                case Effect -> {
                    int c = potionEffectEntry.left().getEffectType().value().getColor();
                    yield new Color(c).a(255);
                }
                case Flat -> {
                    flatColor.get().update();
                    yield flatColor.get();
                }
                case Rainbow -> {
                    localRainbowHue += rainbowSpread.get();
                    int c = java.awt.Color.HSBtoRGB((float) localRainbowHue, rainbowSaturation.get().floatValue(), rainbowBrightness.get().floatValue());
                    yield new Color(c);
                }
            };

            String text = potionEffectEntry.right();
            renderer.text(text, x + alignX(renderer.textWidth(text, shadow.get(), getScale()), alignment.get()), y, color, shadow.get(), getScale());

            y += renderer.textHeight(shadow.get(), getScale());
        }
    }

    private String getString(StatusEffectInstance statusEffectInstance) {
        return String.format("%s %d (%s)", Names.get(statusEffectInstance.getEffectType().value()), statusEffectInstance.getAmplifier() + 1, StatusEffectUtil.getDurationText(statusEffectInstance, 1, mc.world.getTickManager().getTickRate()).getString());
    }

    private double getScale() {
        return customScale.get() ? scale.get() : -1;
    }

    private boolean hasNoVisibleEffects() {
        for (StatusEffectInstance statusEffectInstance : mc.player.getStatusEffects()) {
            if (hiddenEffects.get().contains(statusEffectInstance.getEffectType().value())) continue;
            if (!showAmbient.get() && statusEffectInstance.isAmbient()) continue;
            return false;
        }

        return true;
    }

    public enum ColorMode {
        Effect,
        Flat,
        Rainbow
    }
}
