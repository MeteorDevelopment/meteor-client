/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionMixin {
    @ModifyExpressionValue(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 modifyFluidFlow(Vec3 flow, final Entity entity, final boolean ignoreCurrent) {
        if (entity != mc.player) return flow;

        Velocity velocity = Modules.get().get(Velocity.class);
        if (velocity.isActive() && velocity.liquids.get()) {
            double h = velocity.getHorizontal(velocity.liquidsHorizontal);
            double v = velocity.getVertical(velocity.liquidsVertical);
            flow = flow.multiply(h, v, h);
        }

        return flow;
    }
}
