/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    @Unique private final ThreadLocal<Integer> alphas = new ThreadLocal<>();

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo info) {
        if (Xray.getAlpha(ctx.state(), ctx.pos()) >= 0) info.cancel();
        //int alpha = Xray.getAlpha(ctx.state(), ctx.pos());
        //if (alpha == 0) info.cancel();
        //else alphas.set(alpha);
    }


    //@ModifyArg(method = "writeGeometry", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/api/util/ColorABGR;withAlpha(IF)I"), index = 1)
    //private float setColor(float alpha) {
    //    int alpha0 = alphas.get();
    //    if (alpha0 == 0 || alpha0 == -1) return alpha;
    //    else return alpha0 / 255f;
    //}
}
