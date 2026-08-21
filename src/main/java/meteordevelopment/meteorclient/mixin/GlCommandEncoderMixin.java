/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.backend.opengl.GlCommandEncoder;
import com.mojang.renderpearl.backend.opengl.GlDevice;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin {
    @Shadow
    @Final
    private GlDevice device;

    @SuppressWarnings("deprecation")
    @Inject(method = "createRenderPass", at = @At("RETURN"))
    private void createRenderPass$iGpuDevice(CallbackInfoReturnable<RenderPassBackend> cir) {
        ((IGpuDevice) device).meteor$onCreateRenderPass(cir.getReturnValue());
    }

    // todo ?
//    @Inject(method = "applyPipelineState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_polygonMode(II)V"))
//    private void setPipelineAndApplyState$lineSmooth(RenderPipeline pipeline, CallbackInfo ci) {
//        if (((IRenderPipeline) pipeline).meteor$getLineSmooth()) {
//            glEnable(GL_LINE_SMOOTH);
//            glLineWidth(1);
//        } else {
//            glDisable(GL_LINE_SMOOTH);
//        }
//    }
}
