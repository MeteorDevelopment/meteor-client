/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.world.CollisionShapeEvent;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.CollisionView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockCollisionSpliterator.class)
public abstract class BlockCollisionSpliteratorMixin {
    @WrapOperation(method = "computeNext",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/ShapeContext;getCollisionShape(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/CollisionView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;"
        )
    )
    private VoxelShape onComputeNextCollisionBox(ShapeContext instance, BlockState blockState, CollisionView collisionView, BlockPos blockPos, Operation<VoxelShape> original) {
        VoxelShape shape = original.call(instance, blockState, collisionView, blockPos);

        if (collisionView != MinecraftClient.getInstance().world) {
            return shape;
        }

        CollisionShapeEvent event = MotorClient.EVENT_BUS.post(CollisionShapeEvent.get(blockState, blockPos, shape));
        return event.isCancelled() ? VoxelShapes.empty() : event.shape;
    }
}
