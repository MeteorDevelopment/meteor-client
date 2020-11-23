/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.misc.Nuker;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.block.BlockState;
import net.minecraft.client.particle.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(Particle particle, CallbackInfo info) {
        NoRender noRender = ModuleManager.INSTANCE.get(NoRender.class);

        if (noRender.noBubbles() && (particle instanceof BubbleColumnUpParticle || particle instanceof BubblePopParticle || particle instanceof WaterBubbleParticle)) {
            info.cancel();
        } else if (noRender.noExplosion() && (particle instanceof ExplosionSmokeParticle || particle instanceof ExplosionLargeParticle || particle instanceof ExplosionEmitterParticle)) {
            info.cancel();
        } else if (noRender.noTotemParticles() && particle instanceof TotemParticle) {
            info.cancel();
        }
    }

    @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakParticles(BlockPos blockPos, BlockState state, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(Nuker.class).noParticles() || ModuleManager.INSTANCE.get(NoRender.class).noBlockBreakParticles()) info.cancel();
    }

    @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakingParticles(BlockPos blockPos, Direction direction, CallbackInfo info) {
        if (ModuleManager.INSTANCE.get(Nuker.class).noParticles() || ModuleManager.INSTANCE.get(NoRender.class).noBlockBreakParticles()) info.cancel();
    }
}
