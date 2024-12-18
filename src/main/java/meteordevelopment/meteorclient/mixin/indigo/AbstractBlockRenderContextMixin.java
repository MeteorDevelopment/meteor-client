/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.indigo;

import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlockRenderContext.class)
public abstract class AbstractBlockRenderContextMixin {
    @Final @Shadow(remap = false) protected BlockRenderInfo blockInfo;

    @Inject(method = "renderQuad", at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/render/AbstractBlockRenderContext;bufferQuad(Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;Lnet/minecraft/client/render/VertexConsumer;)V"), cancellable = true)
    private void onBufferQuad(MutableQuadViewImpl quad, CallbackInfo ci) {
        int alpha = Xray.getAlpha(blockInfo.blockState, blockInfo.blockPos);

        if (alpha == 0) ci.cancel();
        else if (alpha != -1) {
            for (int i = 0; i < 4; i++) {
                quad.color(i, rewriteQuadAlpha(quad.color(i), alpha));
            }
        }
    }

    @Unique
    private int rewriteQuadAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
