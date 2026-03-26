/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPassBackend;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import meteordevelopment.meteorclient.mixininterface.IRenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

import static org.lwjgl.opengl.GL11C.*;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderMixin {
    @Unique
    private static final Field METEOR$BACKEND_FIELD;

    static {
        try {
            Class<?> klass = Class.forName("com.mojang.blaze3d.opengl.GlCommandEncoder");
            METEOR$BACKEND_FIELD = klass.getDeclaredField("device");
            METEOR$BACKEND_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("deprecation")
    @Inject(method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPassBackend;", at = @At("RETURN"))
    private void createRenderPass$iGpuDevice(CallbackInfoReturnable<RenderPassBackend> info) {
        try {
            Object device = METEOR$BACKEND_FIELD.get(this);
            if (device instanceof IGpuDevice iGpuDevice) {
                iGpuDevice.meteor$onCreateRenderPass(info.getReturnValue());
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access GlCommandEncoder device.", e);
        }
    }

    @Inject(method = "applyPipelineState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_polygonMode(II)V"))
    private void setPipelineAndApplyState$lineSmooth(RenderPipeline pipeline, CallbackInfo info) {
        if (((IRenderPipeline) pipeline).meteor$getLineSmooth()) {
            glEnable(GL_LINE_SMOOTH);
            glLineWidth(1);
        }
        else {
            glDisable(GL_LINE_SMOOTH);
        }
    }
}
