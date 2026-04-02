/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {FireworkParticles.SparkParticle.class, FireworkParticles.OverlayParticle.class})
public abstract class FireworksSparkParticleSubMixin {
    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void buildExplosionGeometry(QuadParticleRenderState arg, Camera camera, float f, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noFireworkExplosions()) ci.cancel();
    }
}
