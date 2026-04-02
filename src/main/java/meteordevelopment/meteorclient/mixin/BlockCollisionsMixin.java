/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin {
    @WrapOperation(method = "computeNext",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
        )
    )
    private VoxelShape onComputeNextCollisionBox(CollisionContext instance, BlockState blockState, CollisionGetter collisionView, BlockPos blockPos, Operation<VoxelShape> original) {
        VoxelShape shape = original.call(instance, blockState, collisionView, blockPos);

        if (collisionView != Minecraft.getInstance().level) {
            return shape;
        }

        CollisionShapeEvent event = MeteorClient.EVENT_BUS.post(CollisionShapeEvent.get(blockState, blockPos, shape));
        return event.isCancelled() ? Shapes.empty() : event.shape;
    }
}
