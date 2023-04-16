/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    @Unique private final ThreadLocal<Integer> alphas = new ThreadLocal<>();

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(BlockRenderContext ctx, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> info) {
        int alpha = Xray.getAlpha(ctx.state(), ctx.pos());

        if (alpha == 0) info.setReturnValue(false);
        else alphas.set(alpha);
    }

    @Redirect(method = "writeGeometry", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/util/color/ColorABGR;mul(IF)I"))
    private int setColor(int color, float w) {
        int alpha = alphas.get();
        return alpha == -1 ? ColorABGR.mul(color, w) : mul(color, w, alpha);
    }

    // In sodium "mul" removes alpha property, so we make alpha available here
    private static int mul(int color, float w, int a) {
        float r = (float)ColorABGR.unpackRed(color) * w;
        float g = (float)ColorABGR.unpackGreen(color) * w;
        float b = (float)ColorABGR.unpackBlue(color) * w;
        return ColorABGR.pack((int)r, (int)g, (int)b, a);
    }
}
