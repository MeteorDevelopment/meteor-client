/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BreakBlockEvent {
    public BlockPos blockPos;

    public BlockState getBlockState(World world) {
        return world.getBlockState(blockPos);
    }
}
