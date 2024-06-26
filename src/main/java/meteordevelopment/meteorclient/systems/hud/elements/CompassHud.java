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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CompassHud extends HudElement {
    public static final HudElementInfo<CompassHud> INFO = new HudElementInfo<>(Hud.GROUP, "compass", "Displays a compass.", CompassHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTextScale = settings.createGroup("Text Scale");
    private final SettingGroup sgBackground = settings.createGroup("Background");

    // General

    private final Setting<CompassHud.Mode> mode = sgGeneral.add(new EnumSetting.Builder<CompassHud.Mode>()
        .name("type")
        .description("Which type of direction information to show.")
        .defaultValue(Mode.Axis)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 5)
        .onChanged(aDouble -> calculateSize())
        .build()
    );

    private final Setting<SettingColor> colorNorth = sgGeneral.add(new ColorSetting.Builder()
        .name("color-north")
        .description("Color of north.")
        .defaultValue(new SettingColor(225, 45, 45))
        .build()
    );

    private final Setting<SettingColor> colorOther = sgGeneral.add(new ColorSetting.Builder()
        .name("color-north")
        .description("Color of other directions.")
        .defaultValue(new SettingColor())
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Text shadow.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
        .name("border")
        .description("How much space to add around the element.")
        .defaultValue(0)
        .onChanged(integer -> calculateSize())
        .build()
    );

    // Scale

    private final Setting<Boolean> customTextScale = sgTextScale.add(new BoolSetting.Builder()
        .name("custom-text-scale")
        .description("Applies custom text scale rather than the global one.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> textScale = sgTextScale.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Custom text scale.")
        .visible(customTextScale::get)
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

    public CompassHud() {
        super(INFO);

        calculateSize();
    }

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    private void calculateSize() {
        setSize(100 * scale.get(), 100 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = this.x + (getWidth() / 2.0);
        double y = this.y + (getHeight() / 2.0);

        double pitch = isInEditor() ? 120 : MathHelper.clamp(mc.player.getPitch() + 30, -90, 90);
        pitch = Math.toRadians(pitch);

        double yaw = isInEditor() ? 180 : MathHelper.wrapDegrees(mc.player.getYaw());
        yaw = Math.toRadians(yaw);

        for (Direction direction : Direction.values()) {
            String axis = mode.get() == Mode.Axis ? direction.getAxis() : direction.name();

            renderer.text(
                axis,
                (x + getX(direction, yaw)) - (renderer.textWidth(axis, shadow.get(), getTextScale())) / 2,
                (y + getY(direction, yaw, pitch)) - (renderer.textHeight(shadow.get(), getTextScale()) / 2),
                direction == Direction.N ? colorNorth.get() : colorOther.get(),
                shadow.get(),
                getTextScale()
            );
        }

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }
    }

    private double getX(Direction direction, double yaw) {
        return Math.sin(getPos(direction, yaw)) * scale.get() * 40;
    }

    private double getY(Direction direction, double yaw, double pitch) {
        return Math.cos(getPos(direction, yaw)) * Math.sin(pitch) * scale.get() * 40;
    }

    private double getPos(Direction direction, double yaw) {
        return yaw + direction.ordinal() * Math.PI / 2;
    }

    private double getTextScale() {
        return customTextScale.get() ? textScale.get() : -1;
    }

    private enum Direction {
        N("Z-"),
        W("X-"),
        S("Z+"),
        E("X+");

        private final String axis;

        Direction(String axis) {
            this.axis = axis;
        }

        public String getAxis() {
            return axis;
        }
    }

    public enum Mode {
        Direction,
        Axis
    }
}
