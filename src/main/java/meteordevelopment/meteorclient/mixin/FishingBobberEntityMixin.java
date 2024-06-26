/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @WrapOperation(method = "handleStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;pullHookedEntity(Lnet/minecraft/entity/Entity;)V"))
    private void preventFishingRodPull(FishingBobberEntity instance, Entity entity, Operation<Void> original) {
        if (!instance.getWorld().isClient || entity != mc.player) original.call(instance, entity);

        Velocity velocity = Modules.get().get(Velocity.class);
        if (!velocity.isActive() || !velocity.fishing.get()) original.call(instance, entity);
    }
}
