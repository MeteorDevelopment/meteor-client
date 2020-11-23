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

public class HighJump extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
            .name("multiplier")
            .description("Jump height multiplier.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    public HighJump() {
        super(Category.Movement, "high-jump", "Jump higher.");
    }

    public float getMultiplier() {
        return multiplier.get().floatValue();
    }
}
