/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    @Unique
    private final ThreadLocal<Integer> alphas = new ThreadLocal<>();

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo info) {
        int alpha = Xray.getAlpha(ctx.state(), ctx.pos());

        if (alpha == 0) info.cancel();
        else alphas.set(alpha);
    }


    @Inject(method = "writeGeometry", at = @At(value = "FIELD", target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;color:I", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    private void setColor(BlockRenderContext ctx, ChunkModelBuilder builder, Vec3d offset, Material material, BakedQuadView quad, int[] colors, QuadLightData light, CallbackInfo info, ModelQuadOrientation orientation, ChunkVertexEncoder.Vertex[] vertices, ModelQuadFacing normalFace, int dstIndex, int srcIndex, ChunkVertexEncoder.Vertex out) {
        int alpha = alphas.get();

        if (alpha == 0) info.cancel();
        else if (alpha != -1) out.color = ColorABGR.withAlpha(out.color, alpha);
    }
}
