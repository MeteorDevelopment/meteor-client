/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class ShulkerTooltip extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Integer> lines = sgGeneral.add(new IntSetting.Builder()
            .name("lines")
            .description("Number of lines.")
            .defaultValue(8)
            .min(0)
            .build()
    );

    public ShulkerTooltip() {
        super(Category.Misc, "shulker-tooltip", "Better shulker item tooltip.");
    }

    public int lines() {
        return lines.get();
    }
}
