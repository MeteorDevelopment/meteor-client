/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.lithium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper;
import net.caffeinemc.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeperVoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChunkAwareBlockCollisionSweeperVoxelShape.class)
public abstract class ChunkAwareBlockCollisionSweeperMixin extends ChunkAwareBlockCollisionSweeper<VoxelShape> {
    public ChunkAwareBlockCollisionSweeperMixin(Level world, @Nullable Entity entity, AABB box, boolean hideLastCollision) {
        super(world, entity, box, hideLastCollision);
    }

    // TODO(Ravel): target method computeNext with the signature not found
// TODO(Ravel): target method computeNext with the signature not found
    @ModifyExpressionValue(method = "computeNext()Lnet/minecraft/util/shape/VoxelShape;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape modifyCollisionShape(VoxelShape original, @Local(name = "state") BlockState state) {
        if (world != Minecraft.getInstance().world) return original;

        CollisionShapeEvent event = MeteorClient.EVENT_BUS.post(CollisionShapeEvent.get(state, pos, original));
        return event.isCancelled() ? Shapes.empty() : event.shape;
    }
}
