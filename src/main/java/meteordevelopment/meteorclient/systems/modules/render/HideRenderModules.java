/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class HideRenderModules extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    boolean wasTrue = false;
    private final Setting<Boolean> disableOnRestart = sgDefault.add(new BoolSetting.Builder()
        .name("disable-on-restart")
        .description("Disables this module automatically when minecraft is started to prevent confusion")
        .defaultValue(true)
        .build()
    );
    public HideRenderModules() {
        super(Categories.Render, "hide-render-modules", "Attempts to stop all meteor rendering");
        if (disableOnRestart.get() && isActive())
            toggle();
    }
}
