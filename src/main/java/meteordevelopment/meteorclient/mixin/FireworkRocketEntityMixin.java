/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.ElytraBoost;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Shadow
    private int life;

    @Shadow
    private int lifetime;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo info) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework) && this.life > this.lifetime) {
            firework.discard();
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void onEntityHit(EntityHitResult entityHitResult, CallbackInfo info) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework)) {
            firework.discard();
            info.cancel();
        }
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void onBlockHit(BlockHitResult blockHitResult, CallbackInfo info) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework)) {
            firework.discard();
            info.cancel();
        }
    }
}
