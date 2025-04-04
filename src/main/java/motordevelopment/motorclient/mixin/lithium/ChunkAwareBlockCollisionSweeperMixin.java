/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin.lithium;

import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.world.CollisionShapeEvent;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.CollisionView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChunkAwareBlockCollisionSweeper.class)
public abstract class ChunkAwareBlockCollisionSweeperMixin {
    @Redirect(method = "computeNext()Lnet/minecraft/util/shape/VoxelShape;", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/ShapeContext;getCollisionShape(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/CollisionView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;"))
    private VoxelShape onComputeNextCollisionBox(ShapeContext context, BlockState state, CollisionView collisionView, BlockPos pos) {
        VoxelShape shape = context.getCollisionShape(state, collisionView, pos);

        if (collisionView != MinecraftClient.getInstance().world)
            return shape;

        CollisionShapeEvent event = MotorClient.EVENT_BUS.post(CollisionShapeEvent.get(state, pos, shape));
        return event.isCancelled() ? VoxelShapes.empty() : event.shape;
    }
}
