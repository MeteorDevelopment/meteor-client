/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers, ChunkRenderBounds.Builder bounds, CallbackInfo ci) {
        if (Xray.getAlpha(ctx.state(), ctx.pos()) != -1) ci.cancel();
    }
}
