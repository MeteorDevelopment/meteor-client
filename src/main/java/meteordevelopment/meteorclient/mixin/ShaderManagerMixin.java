/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.PipelineCache;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    @Inject(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderManager$PostChainCache;close()V"))
    private void meteor$reloadPipelines(CallbackInfo ci, @Local(ordinal = 0) PipelineCache pipelineCache) {
        MeteorRenderPipelines.precompile(pipelineCache);
    }
}
