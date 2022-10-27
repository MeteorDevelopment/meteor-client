/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BreakBlockEvent extends Cancellable {
    private static final BreakBlockEvent INSTANCE = new BreakBlockEvent();

    public BlockPos blockPos;

    public BlockState getBlockState(World world) {
        return world.getBlockState(blockPos);
    }

    public static BreakBlockEvent get(BlockPos blockPos) {
        INSTANCE.blockPos = blockPos;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
