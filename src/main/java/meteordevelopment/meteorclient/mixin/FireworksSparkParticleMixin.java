/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.particle.FireworksSparkParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworksSparkParticle.FireworkParticle.class)
public abstract class FireworksSparkParticleMixin {
    @Inject(method = "addExplosionParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworksSparkParticle$Explosion;setTrail(Z)V"), cancellable = true)
    private void onAddExplosion(double x, double y, double z, double velocityX, double velocityY, double velocityZ, IntList colors, IntList targetColors, boolean trail, boolean flicker, CallbackInfo info, @Local FireworksSparkParticle.Explosion explosion) {
        if (explosion == null) info.cancel();
    }
}
