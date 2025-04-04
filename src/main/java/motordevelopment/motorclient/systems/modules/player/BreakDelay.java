/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.player;

import motordevelopment.motorclient.events.entity.player.BlockBreakingCooldownEvent;
import motordevelopment.motorclient.events.motor.MouseButtonEvent;
import motordevelopment.motorclient.settings.BoolSetting;
import motordevelopment.motorclient.settings.IntSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.utils.misc.input.KeyAction;
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

    private final Setting<Boolean> noInstaBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("no-insta-break")
        .description("Prevents you from misbreaking blocks if you can instantly break them.")
        .defaultValue(false)
        .build()
    );

    private boolean breakBlockCooldown = false;

    public BreakDelay() {
        super(Categories.Player, "break-delay", "Changes the delay between breaking blocks.");
    }

    @EventHandler
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        if (breakBlockCooldown) {
            event.cooldown = 5;
            breakBlockCooldown = false;
        } else {
            event.cooldown = cooldown.get();
        }
    }

    @EventHandler
    private void onClick(MouseButtonEvent event) {
        if (event.action == KeyAction.Press && noInstaBreak.get()) {
            breakBlockCooldown = true;
        }
    }

    public boolean preventInstaBreak() {
        return isActive() && noInstaBreak.get();
    }
}
