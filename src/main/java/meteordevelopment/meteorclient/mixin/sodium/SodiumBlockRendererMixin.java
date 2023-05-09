/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexEncoder;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    @Unique private final ThreadLocal<Integer> alphas = new ThreadLocal<>();

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(BlockRenderContext ctx, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> info) {
        int alpha = Xray.getAlpha(ctx.state(), ctx.pos());

        if (alpha == 0) info.setReturnValue(false);
        else alphas.set(alpha);
    }


    @Inject(method = "writeGeometry", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/util/color/ColorABGR;mul(IF)I", shift = At.Shift.BY, by = 2), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void setColor(BlockRenderContext ctx, ChunkVertexBufferBuilder vertexBuffer, IndexBufferBuilder indexBuffer, Vec3d offset, ModelQuadView quad, int[] colors, float[] brightness, int[] lightmap, CallbackInfo info, ModelQuadOrientation orientation, ChunkVertexEncoder.Vertex[] vertices, int dstIndex, int srcIndex, ChunkVertexEncoder.Vertex out) {
        int alpha = alphas.get();

        if (alpha == 0) info.cancel();
        else if (alpha != -1) {
            out.color &= 0xFFFFFF;
            out.color |= alpha << 24;
        }
    }
}
