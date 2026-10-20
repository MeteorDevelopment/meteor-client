/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mc.MixinTargets;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    @Inject(method = MixinTargets.SHADER_MANAGER_APPLY, at = @At("TAIL"))
    private void meteor$reloadPipelines(CallbackInfo ci) {
        MeteorRenderPipelines.precompile();
    }
}
