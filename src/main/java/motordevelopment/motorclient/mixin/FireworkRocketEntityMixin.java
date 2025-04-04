/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.movement.ElytraBoost;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
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
    private int lifeTime;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo info) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework) && this.life > this.lifeTime) {
            firework.discard();
        }
    }

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void onEntityHit(EntityHitResult entityHitResult, CallbackInfo info) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework)) {
            firework.discard();
            info.cancel();
        }
    }

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    private void onBlockHit(BlockHitResult blockHitResult, CallbackInfo info) {
        FireworkRocketEntity firework = ((FireworkRocketEntity) (Object) this);

        if (Modules.get().get(ElytraBoost.class).isFirework(firework)) {
            firework.discard();
            info.cancel();
        }
    }
}
