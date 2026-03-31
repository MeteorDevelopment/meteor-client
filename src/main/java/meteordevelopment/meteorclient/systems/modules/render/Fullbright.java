/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MobEffectInstanceAccessor;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LightLayer;

public class Fullbright extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode to use for Fullbright.")
        .defaultValue(Mode.Gamma)
        .onChanged(mode -> {
            if (isActive()) {
                if (mode != Mode.Potion) disableNightVision();
                if (mc.worldRenderer != null) mc.worldRenderer.reload();
            }
        })
        .build()
    );

    public final Setting<LightLayer> lightType = sgGeneral.add(new EnumSetting.Builder<LightLayer>()
        .name("light-type")
        .description("Which type of light to use for Luminance mode.")
        .defaultValue(LightLayer.BLOCK)
        .visible(() -> mode.get() == Mode.Luminance)
        .onChanged(integer -> {
            if (mc.worldRenderer != null && isActive()) mc.worldRenderer.reload();
        })
        .build()
    );

    private final Setting<Integer> minimumLightLevel = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-light-level")
        .description("Minimum light level when using Luminance mode.")
        .visible(() -> mode.get() == Mode.Luminance)
        .defaultValue(8)
        .range(0, 15)
        .sliderMax(15)
        .onChanged(integer -> {
            if (mc.worldRenderer != null && isActive()) mc.worldRenderer.reload();
        })
        .build()
    );

    public Fullbright() {
        super(Categories.Render, "fullbright", "Lights up your world!");
    }

    @Override
    public void onActivate() {
        if (mode.get() == Mode.Luminance) mc.worldRenderer.reload();
    }

    @Override
    public void onDeactivate() {
        if (mode.get() == Mode.Luminance) mc.worldRenderer.reload();
        else if (mode.get() == Mode.Potion) disableNightVision();
    }

    public int getLuminance(LightLayer type) {
        if (!isActive() || mode.get() != Mode.Luminance || type != lightType.get()) return 0;
        return minimumLightLevel.get();
    }

    public boolean getGamma() {
        return isActive() && mode.get() == Mode.Gamma;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || !mode.get().equals(Mode.Potion)) return;
        if (mc.player.hasStatusEffect(BuiltInRegistries.STATUS_EFFECT.getEntry(MobEffects.NIGHT_VISION.value()))) {
            MobEffectInstance instance = mc.player.getStatusEffect(BuiltInRegistries.STATUS_EFFECT.getEntry(MobEffects.NIGHT_VISION.value()));
            if (instance != null && instance.getDuration() < 420)
                ((MobEffectInstanceAccessor) instance).meteor$setDuration(420);
        } else {
            mc.player.addStatusEffect(new StatusEffectInstance(BuiltInRegistries.STATUS_EFFECT.getEntry(MobEffects.NIGHT_VISION.value()), 420, 0));
        }
    }

    private void disableNightVision() {
        if (mc.player == null) return;
        if (mc.player.hasStatusEffect(BuiltInRegistries.STATUS_EFFECT.getEntry(MobEffects.NIGHT_VISION.value()))) {
            mc.player.removeStatusEffect(BuiltInRegistries.STATUS_EFFECT.getEntry(MobEffects.NIGHT_VISION.value()));
        }
    }

    public enum Mode {
        Gamma,
        Potion,
        Luminance
    }
}
