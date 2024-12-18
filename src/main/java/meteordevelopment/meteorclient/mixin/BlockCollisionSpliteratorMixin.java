/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
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

        CollisionShapeEvent event = MeteorClient.EVENT_BUS.post(CollisionShapeEvent.get(blockState, blockPos, shape));
        return event.isCancelled() ? VoxelShapes.empty() : event.shape;
    }
}
