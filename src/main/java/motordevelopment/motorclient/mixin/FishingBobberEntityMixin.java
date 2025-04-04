/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.Velocity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static motordevelopment.motorclient.MotorClient.mc;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @WrapOperation(method = "handleStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/FishingBobberEntity;pullHookedEntity(Lnet/minecraft/entity/Entity;)V"))
    private void preventFishingRodPull(FishingBobberEntity instance, Entity entity, Operation<Void> original) {
        if (!instance.getWorld().isClient || entity != mc.player) original.call(instance, entity);

        Velocity velocity = Modules.get().get(Velocity.class);
        if (!velocity.isActive() || !velocity.fishing.get()) original.call(instance, entity);
    }
}
