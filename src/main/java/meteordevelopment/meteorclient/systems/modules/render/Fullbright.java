/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Fullbright extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("The mode to use for Fullbright.")
            .defaultValue(Mode.Gamma)
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
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
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
    }

    public int getLuminance() {
        if (!isActive() || mode.get() != Mode.Luminance) return 0;
        return minimumLightLevel.get();
    }

    public boolean getGamma() {
        return isActive() && mode.get() == Mode.Gamma;
    }

    public enum Mode {
        Gamma,
        Luminance
    }
}
