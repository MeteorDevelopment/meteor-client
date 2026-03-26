/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionMixin {
    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 meteor$modifyFluidFlow(FluidState state, BlockGetter world, BlockPos pos, Entity entity, boolean ignoreFluidPush) {
        Vec3 vec = state.getFlow(world, pos);
        if (entity != mc.player) return vec;

        Velocity velocity = Modules.get().get(Velocity.class);
        if (velocity.isActive() && velocity.liquids.get()) {
            vec = vec.multiply(
                velocity.getHorizontal(velocity.liquidsHorizontal),
                velocity.getVertical(velocity.liquidsVertical),
                velocity.getHorizontal(velocity.liquidsHorizontal)
            );
        }

        return vec;
    }
}
