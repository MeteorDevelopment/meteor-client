/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Unique
    private float[] identity;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        identity = new float[4 * 4];
        new Matrix4f().get(identity);
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/SimpleFramebuffer;endWrite()V", shift = At.Shift.BEFORE))
    private void onUpdate(CallbackInfo info) {
        if (Modules.get().get(Fullbright.class).getGamma() || Modules.get().isActive(Xray.class)) {
            ShaderProgram program = Objects.requireNonNull(RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR), "Position color shader not loaded");
            program.getUniformOrDefault("ProjMat").set(identity);
            program.getUniformOrDefault("ModelViewMat").set(identity);
            program.getUniformOrDefault("ColorModulator").set(1f, 1f, 1f, 1f);

            BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(-1, -1, 0).color(0xFFFFFFFF);
            bufferBuilder.vertex( 1, -1, 0).color(0xFFFFFFFF);
            bufferBuilder.vertex( 1,  1, 0).color(0xFFFFFFFF);
            bufferBuilder.vertex(-1,  1, 0).color(0xFFFFFFFF);

            program.bind();
            BufferRenderer.draw(bufferBuilder.end());
            program.unbind();
        }
    }

    @Inject(method = "getDarknessFactor(F)F", at = @At("HEAD"), cancellable = true)
	private void getDarknessFactor(float tickDelta, CallbackInfoReturnable<Float> info) {
		if (Modules.get().get(NoRender.class).noDarkness()) info.setReturnValue(0.0f);
	}
}
