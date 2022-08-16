/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.particle.FireworksSparkParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FireworksSparkParticle.FireworkParticle.class)
public class FireworksSparkParticleMixin {
    @Inject(method = "addExplosionParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworksSparkParticle$Explosion;setTrail(Z)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onAddExplosion(double x, double y, double z, double velocityX, double velocityY, double velocityZ, int[] colors, int[] fadeColors, boolean trail, boolean flicker, CallbackInfo info, FireworksSparkParticle.Explosion explosion) {
        if (explosion == null) info.cancel();
    }
}
