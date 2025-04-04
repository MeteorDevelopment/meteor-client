/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.systems.modules.world;

import motordevelopment.motorclient.events.entity.player.BreakBlockEvent;
import motordevelopment.motorclient.events.entity.player.PlaceBlockEvent;
import motordevelopment.motorclient.settings.BoolSetting;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.settings.SettingGroup;
import motordevelopment.motorclient.systems.modules.Categories;
import motordevelopment.motorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;

public class NoGhostBlocks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> breaking = sgGeneral.add(new BoolSetting.Builder()
        .name("breaking")
        .description("Whether to apply for block breaking actions.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> placing = sgGeneral.add(new BoolSetting.Builder()
        .name("placing")
        .description("Whether to apply for block placement actions.")
        .defaultValue(true)
        .build()
    );

    public NoGhostBlocks() {
        super(Categories.World, "no-ghost-blocks", "Attempts to prevent ghost blocks arising.");
    }

    @EventHandler
    private void onBreakBlock(BreakBlockEvent event) {
        if (mc.isInSingleplayer() || !breaking.get()) return;

        event.cancel();

        BlockState blockState = mc.world.getBlockState(event.blockPos);
        blockState.getBlock().onBreak(mc.world, event.blockPos, blockState, mc.player);
    }

    @EventHandler
    private void onPlaceBlock(PlaceBlockEvent event) {
        if (!placing.get()) return;

        event.cancel();
    }
}
