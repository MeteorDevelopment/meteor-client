/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.modules;

import meteordevelopment.meteorclient.mixin.ClientPlayerInteractionManagerAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HUD;

public class BreakingBlockHud extends DoubleTextHudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> hide = sgGeneral.add(new BoolSetting.Builder()
        .name("hide")
        .description("Hide while not breaking any block.")
        .defaultValue(true)
        .build()
    );

    public BreakingBlockHud(HUD hud) {
        super(hud, "breaking-block", "Displays percentage of the block you are breaking.", "Breaking Block: ");
    }

    @Override
    protected String getRight() {
        if (isInEditor()) {
            visible = true;
            return "0%";
        }

        float breakingProgress = ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).getBreakingProgress();
        if (hide.get()) visible = breakingProgress > 0;
        return String.format("%.0f%%", breakingProgress * 100);
    }
}
