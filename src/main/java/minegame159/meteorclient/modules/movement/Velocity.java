/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Velocity extends ToggleModule {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Double> horizontal = sg.add(new DoubleSetting.Builder()
            .name("horizontal-multiplier")
            .description("How much velocity to apply horizontally.")
            .defaultValue(0)
            .min(0)
            .max(1)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    private final Setting<Double> vertical = sg.add(new DoubleSetting.Builder()
            .name("vertical-multiplier")
            .description("How much velocity to apply vertically.")
            .defaultValue(0)
            .min(0)
            .max(1)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    public Velocity() {
        super(Category.Movement, "velocity", "Prevents you from getting pushed by mobs, taking damage, etc.");
    }

    public double getHorizontal() {
        return isActive() ? horizontal.get() : 1;
    }

    public double getVertical() {
        return isActive() ? vertical.get() : 1;
    }
}
