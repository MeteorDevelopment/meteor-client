/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class AntiLevitation extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    private final Setting<Boolean> applyGravity = sgGeneral.add(new BoolSetting.Builder()
            .name("apply-gravity")
            .description("Apply gravity.")
            .defaultValue(false)
            .build()
    );

    public AntiLevitation() {
        super(Category.Movement, "anti-levitation", "Removes levitation effect.");
    }

    public boolean isApplyGravity() {
        return applyGravity.get();
    }
}
