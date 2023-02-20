/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.BlockBreakingCooldownEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class BreakDelay extends Module {
    SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Block break cooldown in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(5)
        .build()
    );

    public final Setting<Boolean> noInstaBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("no-insta-break")
        .description("Prevent you from breaking blocks instantly.")
        .defaultValue(false)
        .build()
    );

    public BreakDelay() {
        super(Categories.Player, "break-delay", "Changes the delay between breaking blocks.");
    }

    @EventHandler()
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        event.cooldown = cooldown.get();
    }
}
