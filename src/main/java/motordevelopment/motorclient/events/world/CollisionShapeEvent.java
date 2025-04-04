/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.world;

import com.mojang.blaze3d.systems.RenderSystem;
import motordevelopment.motorclient.events.Cancellable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class CollisionShapeEvent extends Cancellable {
    private static final CollisionShapeEvent INSTANCE = new CollisionShapeEvent();

    public BlockState state;
    public BlockPos pos;
    public VoxelShape shape;

    public static CollisionShapeEvent get(BlockState state, BlockPos pos, VoxelShape shape) {
        CollisionShapeEvent event = INSTANCE;

        if (!RenderSystem.isOnRenderThread()) {
            event = new CollisionShapeEvent();
        }

        event.setCancelled(false);
        event.state = state;
        event.pos = pos;
        event.shape = shape;

        return event;
    }
}
