/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.particle.FireworksSparkParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FireworksSparkParticle.FireworkParticle.class)
public class FireworksSparkParticleMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;setColor(FFF)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onAddFlash(CallbackInfo info, int j, NbtCompound nbtCompound2, FireworkRocketItem.Type type, boolean bl3, boolean bl4, int[] is, int[] js, int k, float f, float g, float h, Particle particle) {
        if (particle == null) info.cancel();
    }

    @Inject(method = "addExplosionParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/FireworksSparkParticle$Explosion;setTrail(Z)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onAddExplosion(double x, double y, double z, double velocityX, double velocityY, double velocityZ, int[] colors, int[] fadeColors, boolean trail, boolean flicker, CallbackInfo info, FireworksSparkParticle.Explosion explosion) {
        if (explosion == null) info.cancel();
    }
}
