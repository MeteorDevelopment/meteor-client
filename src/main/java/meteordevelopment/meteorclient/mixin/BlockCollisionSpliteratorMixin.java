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
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockCollisionSpliterator.class)
public abstract class BlockCollisionSpliteratorMixin {

    @WrapOperation(method = "computeNext",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;"
        )
    )
    private VoxelShape onComputeNextCollisionBox(BlockState state, BlockView world, BlockPos pos, ShapeContext context, Operation<VoxelShape> original) {
        VoxelShape shape = original.call(state, world, pos, context);

        if (world != MinecraftClient.getInstance().world) {
            return shape;
        }

        CollisionShapeEvent event = MeteorClient.EVENT_BUS.post(CollisionShapeEvent.get(state, pos, shape));
        return event.isCancelled() ? VoxelShapes.empty() : event.shape;
    }
}
