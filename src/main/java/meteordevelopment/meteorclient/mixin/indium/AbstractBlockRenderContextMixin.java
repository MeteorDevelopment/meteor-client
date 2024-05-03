/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin.indium;

import link.infra.indium.renderer.mesh.MutableQuadViewImpl;
import link.infra.indium.renderer.render.AbstractBlockRenderContext;
import link.infra.indium.renderer.render.BlockRenderInfo;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public abstract class AbstractBlockRenderContextMixin {
    @Shadow
    protected BlockRenderInfo blockInfo;

    @Inject(method = "renderQuad(Llink/infra/indium/renderer/mesh/MutableQuadViewImpl;Z)V", at = @At(value = "INVOKE", target = "Llink/infra/indium/renderer/render/AbstractBlockRenderContext;bufferQuad(Llink/infra/indium/renderer/mesh/MutableQuadViewImpl;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;)V"), cancellable = true)
    private void onBufferQuad(MutableQuadViewImpl quad, boolean isVanilla, CallbackInfo ci) {
        int alpha = Xray.getAlpha(blockInfo.blockState, blockInfo.blockPos);

        if (alpha == 0) ci.cancel();
    }
}
