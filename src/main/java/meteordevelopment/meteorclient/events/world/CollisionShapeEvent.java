/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.world;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class CollisionShapeEvent extends Cancellable {
    public enum CollisionType {
        BLOCK,
        FLUID
    }

    private static final CollisionShapeEvent INSTANCE = new CollisionShapeEvent();

    public BlockState state;
    public BlockPos pos;
    public VoxelShape shape;
    public CollisionType type;

    public static CollisionShapeEvent get(BlockState state, BlockPos pos, VoxelShape shape, CollisionType type) {
        INSTANCE.state = state;
        INSTANCE.pos = pos;
        INSTANCE.shape = shape;
        INSTANCE.type = type;
        return INSTANCE;
    }
}
