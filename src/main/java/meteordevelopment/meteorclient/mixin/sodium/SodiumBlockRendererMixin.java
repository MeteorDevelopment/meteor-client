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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin {
    @Unique private final ThreadLocal<Integer> alphas = new ThreadLocal<>();
    @Unique private final ThreadLocal<int[]> colors = ThreadLocal.withInitial(() -> new int[4]);

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(BlockRenderContext ctx, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> info) {
        int alpha = Xray.getAlpha(ctx.state(), ctx.pos());

        if (alpha == 0) info.setReturnValue(false);
        else alphas.set(alpha);
    }

    // TODO: Looks like Sodium disables transparency on blocks that aren't supposed to be transparent
    @ModifyVariable(method = "writeGeometry", at = @At("HEAD"), argsOnly = true, index = 6)
    private int[] onWriteGeometryModifyColors(int[] colors) {
        int alpha = alphas.get();

        if (alpha != -1) {
            if (colors == null) {
                colors = this.colors.get();
                Arrays.fill(colors, ColorABGR.pack(255, 255, 255, alpha));
            }
            else {
                for (int i = 0; i < colors.length; i++) {
                    colors[i] = ColorABGR.withAlpha(colors[i], alpha / 255f);
                }
            }
        }

        return colors;
    }
}
