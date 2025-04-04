/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.Fullbright;
import motordevelopment.motorclient.systems.modules.render.NoRender;
import motordevelopment.motorclient.systems.modules.render.Xray;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    @Final
    private SimpleFramebuffer lightmapFramebuffer;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/SimpleFramebuffer;endWrite()V", shift = At.Shift.BEFORE))
    private void onUpdate(CallbackInfo info) {
        if (Modules.get().get(Fullbright.class).getGamma() || Modules.get().isActive(Xray.class)) {
            this.lightmapFramebuffer.clear();
        }
    }

    @Inject(method = "getDarknessFactor(F)F", at = @At("HEAD"), cancellable = true)
	private void getDarknessFactor(float tickDelta, CallbackInfoReturnable<Float> info) {
		if (Modules.get().get(NoRender.class).noDarkness()) info.setReturnValue(0.0f);
	}
}
