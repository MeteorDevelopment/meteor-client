/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerModelHud extends HudElement {
    public static final HudElementInfo<PlayerModelHud> INFO = new HudElementInfo<>(Hud.GROUP, "player-model", "Displays a model of your player.", PlayerModelHud::new);
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<Boolean> copyYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-yaw")
        .description("Makes the player model's yaw equal to yours.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> customYaw = sgGeneral.add(new IntSetting.Builder()
        .name("custom-yaw")
        .description("Custom yaw for when copy yaw is off.")
        .defaultValue(0)
        .range(-180, 180)
        .sliderRange(-180, 180)
        .visible(() -> !copyYaw.get())
        .build()
    );

    private final Setting<Boolean> copyPitch = sgGeneral.add(new BoolSetting.Builder()
        .name("copy-pitch")
        .description("Makes the player model's pitch equal to yours.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> customPitch = sgGeneral.add(new IntSetting.Builder()
        .name("custom-pitch")
        .description("Custom pitch for when copy pitch is off.")
        .defaultValue(0)
        .range(-90, 90)
        .sliderRange(-90, 90)
        .visible(() -> !copyPitch.get())
        .build()
    );

    private final Setting<CenterOrientation> centerOrientation = sgGeneral.add(new EnumSetting.Builder<CenterOrientation>()
        .name("center-orientation")
        .description("Which direction the player faces when the HUD model faces directly forward.")
        .defaultValue(CenterOrientation.South)
        .build()
    );

    // Scale

    public final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .onChanged(aBoolean -> calculateSize())
        .build()
    );

    public final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(2)
        .onChanged(aDouble -> calculateSize())
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

    public PlayerModelHud() {
        super(INFO);

        calculateSize();
    }

    @Override
    public void render(HudRenderer renderer) {
        renderer.post(() -> {
            PlayerEntity player = mc.player;
            if (player == null) return;

            float offsetYaw = centerOrientation.get() == CenterOrientation.North ? 180 : 0;
            float yaw = copyYaw.get() ? MathHelper.wrapDegrees(player.lastYaw + (player.getYaw() - player.lastYaw) * mc.getRenderTickCounter().getTickProgress(true) + offsetYaw) : (float) customYaw.get();
            float pitch = copyPitch.get() ? player.getPitch() : (float) customPitch.get();

            renderer.entity(player, x, y, getWidth(), getHeight(), -yaw, -pitch);
        });

        if (background.get()) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
        }
        else if (mc.player == null) {
            renderer.quad(x, y, getWidth(), getHeight(), backgroundColor.get());
            renderer.line(x, y, x + getWidth(), y + getHeight(), Color.GRAY);
            renderer.line(x + getWidth(), y, x, y + getHeight(), Color.GRAY);
        }
    }

    private void calculateSize() {
        setSize(50 * getScale(), 75 * getScale());
    }

    private double getScale() {
        return customScale.get() ? scale.get() : scale.getDefaultValue();
    }

    private enum CenterOrientation {
        North,
        South
    }
}
