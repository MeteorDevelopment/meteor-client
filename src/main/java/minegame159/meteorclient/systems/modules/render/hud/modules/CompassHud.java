/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.hud.modules;

import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.render.hud.HUD;
import minegame159.meteorclient.systems.modules.render.hud.HudRenderer;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.MathHelper;

public class CompassHud extends HudElement {
    public enum Mode {
        Axis,
        Pole
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mod = sgGeneral.add(new EnumSetting.Builder<CompassHud.Mode>()
            .name("mode")
            .description("The mode of the compass.")
            .defaultValue(CompassHud.Mode.Pole)
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of compass.")
            .defaultValue(1)
            .sliderMin(1)
            .sliderMax(5)
            .build()
    );

    private final Setting<SettingColor> northColor = sgGeneral.add(new ColorSetting.Builder()
            .name("north-color")
            .description("The color of north axis.")
            .defaultValue(new SettingColor(225, 45, 45))
            .build()
    );

    private final Setting<SettingColor> otherColor = sgGeneral.add(new ColorSetting.Builder()
            .name("other-color")
            .description("The color of other axis.")
            .defaultValue(new SettingColor(225, 225, 255))
            .build()
    );

    public CompassHud(HUD hud) {
        super(hud, "compass", "Displays your rotation as a 3D compass.");
    }

    @Override
    public void update(HudRenderer renderer) {
        box.setSize(100 *  scale.get(), 100 *  scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();

        for (Direction dir : Direction.values()) {
            double pos = getPosOnCompass(dir);
            renderer.text(mod.get() == Mode.Axis ? dir.getAlternate() : dir.name(), (x + (box.width / 2.0)) + getX(pos), (y + (box.height / 2.0)) + getY(pos), (dir == Direction.N) ? northColor.get() : otherColor.get());
        }
    }

    private double getX(double rad) {
        return Math.sin(rad) * (scale.get() * 40);
    }

    private double getY(double rad) {
        double pitch = 0;
        if (!isInEditor()) pitch = mc.player.pitch;

        return Math.cos(rad) * Math.sin(Math.toRadians(MathHelper.clamp(pitch + 30.0f, -90.0f, 90.0f))) * (scale.get() * 40);
    }

    private double getPosOnCompass(Direction dir) {
        double yaw = 0;
        if (!isInEditor()) yaw = mc.player.yaw;

        return Math.toRadians(MathHelper.wrapDegrees(yaw)) + dir.ordinal() * 1.5707963267948966;
    }

    private enum Direction {
        N("Z-"),
        W("X-"),
        S("Z+"),
        E("X+");

        String alternate;

        Direction(String alternate) {
            this.alternate = alternate;
        }

        public String getAlternate() {
            return alternate;
        }
    }
}