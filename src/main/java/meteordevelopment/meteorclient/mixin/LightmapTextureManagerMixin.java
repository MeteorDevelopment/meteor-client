/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    private boolean needsUpdate;

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void update$skip(LightmapRenderState state, float tickProgress, CallbackInfo ci) {
        if (Modules.get() == null) return;

        if (Modules.get().get(Fullbright.class).getGamma() || Modules.get().isActive(Xray.class)) {
            state.needsUpdate = needsUpdate;
            if (!needsUpdate) {
                ci.cancel();
                return;
            }

            state.blockFactor = 1.4f;
            state.blockLightTint = LightmapRenderStateExtractor.WHITE;
            state.skyFactor = 1.0f;
            state.skyLightColor = LightmapRenderStateExtractor.WHITE;
            state.ambientColor = LightmapRenderStateExtractor.WHITE;
            state.brightness = 1.0f;
            state.darknessEffectScale = 0.0f;
            state.nightVisionEffectIntensity = 0.0f;
            state.nightVisionColor = LightmapRenderStateExtractor.WHITE;
            state.bossOverlayWorldDarkening = 0.0f;
            needsUpdate = false;
            ci.cancel();
        }
    }

    @Inject(method = "calculateDarknessScale", at = @At("HEAD"), cancellable = true)
    private void getDarknessFactor(LivingEntity entity, float factor, float tickProgress, CallbackInfoReturnable<Float> info) {
        if (Modules.get() != null && Modules.get().get(NoRender.class).noDarkness()) info.setReturnValue(0.0f);
    }
}
