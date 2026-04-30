/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    @Nullable
    protected abstract <T extends ParticleOptions> Particle makeParticle(T options, double x, double y, double z, double xa, double ya, double za);

    @Inject(method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void onCreateParticle(ParticleOptions options, double x, double y, double z, double xa, double ya, double za, CallbackInfoReturnable<Particle> cir) {
        ParticleEvent event = MeteorClient.EVENT_BUS.post(ParticleEvent.get(options));

        if (event.isCancelled()) {
            if (options.getType() == ParticleTypes.FLASH)
                cir.setReturnValue(makeParticle(options, x, y, z, xa, ya, za));
            else cir.cancel();
        }
    }
}
