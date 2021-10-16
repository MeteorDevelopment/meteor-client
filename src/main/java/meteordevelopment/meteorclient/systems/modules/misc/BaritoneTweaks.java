/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import baritone.api.BaritoneAPI;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class BaritoneTweaks extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> smartSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-sprint")
        .description("Sprint only with enough food saturation.")
        .defaultValue(true)
        .build()
    );

    public BaritoneTweaks() {
        super(Categories.Misc, "baritone-tweaks", "Various baritone related utilities.");
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (smartSprint.get()) {
            if (mc.player.getHungerManager().getFoodLevel() >= 8) BaritoneAPI.getSettings().allowSprint.value = true;
            else BaritoneAPI.getSettings().allowSprint.value = false;
        }
    }

}
