/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class Reach extends ToggleModule {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Double> reach = sg.add(new DoubleSetting.Builder()
            .name("reach")
            .description("Reach.")
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
    );

    public Reach() {
        super(Category.Player, "reach", "Modifies your reach.");
    }

    public float getReach() {
        return reach.get().floatValue();
    }
}
