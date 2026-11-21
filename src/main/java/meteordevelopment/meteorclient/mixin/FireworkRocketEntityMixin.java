/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.ElytraBoost;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Shadow
    private int life;

    @Shadow
    private int lifetime;

    @Shadow
    private LivingEntity attachedToEntity;

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 fixRocketBoostDirection(Vec3 original) {
        if (attachedToEntity == mc.player && mc.player != null && Rotations.shouldCorrectPlayerInput()) return Rotations.getServerDirection();
        return original;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework) && this.life > this.lifetime) {
            firework.discard();
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void onHitEntity(EntityHitResult hitResult, CallbackInfo ci) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework)) {
            firework.discard();
            ci.cancel();
        }
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void onHitBlock(BlockHitResult hitResult, CallbackInfo ci) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework)) {
            firework.discard();
            ci.cancel();
        }
    }
}
