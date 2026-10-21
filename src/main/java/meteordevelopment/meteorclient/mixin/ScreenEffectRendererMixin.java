/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void onRenderFireOverlay(CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noFireOverlay()) ci.cancel();
    }

    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void onRenderUnderwaterOverlay(CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noLiquidOverlay()) ci.cancel();
    }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void render(CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noInWallOverlay()) ci.cancel();
    }
}
