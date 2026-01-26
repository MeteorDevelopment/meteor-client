/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import meteordevelopment.meteorclient.mixininterface.IGlCommandEncoder;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import meteordevelopment.meteorclient.mixininterface.IRenderPipeline;
import meteordevelopment.meteorclient.renderer.texture.AnimatedNativeImage;
import net.minecraft.client.gl.*;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.opengl.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.opengl.GL11C.*;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin implements IGlCommandEncoder {
    @Shadow
    @Final
    private GlBackend backend;

    @Shadow
    private boolean renderPassOpen;

    @SuppressWarnings("deprecation")
    @Inject(method = "createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;", at = @At("RETURN"))
    private void createRenderPass$iGpuDevice(CallbackInfoReturnable<RenderPass> info) {
        ((IGpuDevice) backend).meteor$onCreateRenderPass(info.getReturnValue());
    }

    @Inject(method = "setPipelineAndApplyState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_polygonMode(II)V"))
    private void setPipelineAndApplyState$lineSmooth(RenderPipeline pipeline, CallbackInfo info) {
        if (((IRenderPipeline) pipeline).meteor$getLineSmooth()) {
            glEnable(GL_LINE_SMOOTH);
            glLineWidth(1);
        }
        else {
            glDisable(GL_LINE_SMOOTH);
        }
    }

    @Unique
    public void meteor_client$writeToAnimTexture(GpuTexture gpuTexture, AnimatedNativeImage nativeImage) {
        int width = gpuTexture.getWidth(0);
        int height = gpuTexture.getHeight(0);
        int layers = gpuTexture.getDepthOrLayers();
        if (nativeImage.getWidth() != width || nativeImage.getHeight() != height) {
            throw new IllegalArgumentException(
                "Cannot replace texture of size " + width + "x" + height + "x" + layers + " with image of size "
                    + nativeImage.getWidth() + "x" + nativeImage.getHeight() + "x" + nativeImage.getLayers()
            );
        } else if (gpuTexture.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
        } else if ((gpuTexture.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
        } else {
            this.meteor_client$writeToAnimTexture(gpuTexture, nativeImage, 0, layers, 0, 0, 0, width, height, 0, 0, 0);
        }
    }

    @Unique
    public void meteor_client$writeToAnimTexture(GpuTexture gpuTexture, AnimatedNativeImage nativeImage, int mipLevel, int layer, int dstOffsetX,
                                                 int dstOffsetY, int dstOffsetZ, int width, int height, int srcOffsetX, int srcOffsetY, int srcOffsetZ) {
        if (this.renderPassOpen) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (mipLevel >= 0 && mipLevel < gpuTexture.getMipLevels()) {
            if (srcOffsetX + width > nativeImage.getWidth() || srcOffsetY + height > nativeImage.getHeight() || srcOffsetZ + layer > nativeImage.getLayers()) {
                throw new IllegalArgumentException(
                    "Copy source (" + nativeImage.getWidth() + "x" + nativeImage.getHeight() + ") is not large enough to read a rectangle of "
                        + width + "x" + height + "x" + layer + " from " + srcOffsetX + "x" + srcOffsetY + "x" + srcOffsetZ);
            } else if (dstOffsetX + width > gpuTexture.getWidth(mipLevel) || dstOffsetY + height > gpuTexture.getHeight(mipLevel) || srcOffsetZ + layer > gpuTexture.getDepthOrLayers() >> mipLevel){
                throw new IllegalArgumentException(
                    "Dest texture (" + width + "x" + height + ") is not large enough to write a rectangle of " + width + "x" + height + "x" + layer +" at "
                        + dstOffsetX + "x" + dstOffsetY + "x" + dstOffsetZ + " (at mip level " + mipLevel + ")"
                );
            } else if (gpuTexture.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((gpuTexture.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            } else if (layer > gpuTexture.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + gpuTexture.getDepthOrLayers());
            } else {
                if ((gpuTexture.usage() & 16) != 0) {
                    throw new UnsupportedOperationException("Cubemap compatible textures aren't implemented yet");
                }
                int target = GL30.GL_TEXTURE_2D_ARRAY;
                NativeImage.Format textureFormat = nativeImage.getFormat();
                int glId = ((GlTexture) gpuTexture).getGlId();
                GL11.glBindTexture(target, glId);

                GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, nativeImage.getWidth());
                GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, srcOffsetX);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, srcOffsetY);
                GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, nativeImage.getFormat().getChannelCount());

                // FIXME should have a method in GlStateManager for this. (Another duck?)
                GL12.glTexSubImage3D(target,mipLevel,dstOffsetX,dstOffsetY,dstOffsetZ,width,height,layer,
                    GlConst.toGl(textureFormat), GlConst.GL_UNSIGNED_BYTE, nativeImage.imageId());
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + gpuTexture.getMipLevels());
        }
    }

    @ModifyArg(method = "setupRenderPass", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_texParameter(III)V"), index = 0)
    private int onTexParameterOne(int target, @Local(ordinal = 0) GlTexture glTexture) {
        if (glTexture.getDepthOrLayers() > 1) return GL30.GL_TEXTURE_2D_ARRAY;
        return target;
    }

    @Redirect(
        method = "setupRenderPass",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_bindTexture(I)V")
    )
    private void meteor$bindTextureCorrectTarget(int id, @Local(ordinal = 0) GlTexture glTexture) {
        if (glTexture.getDepthOrLayers() > 1) { //(glTexture.getLabel().contains("gif")) {
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, id);
        } else {
            GlStateManager._bindTexture(id);
        }
    }
}
