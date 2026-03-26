/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockFeatureRenderer.class)
public abstract class BlockRenderManagerMixin {
    @Inject(method = "renderBreakingBlockModelSubmits", at = @At("HEAD"), cancellable = true)
    private void renderDamage(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, CallbackInfo info) {
        if (Modules.get().isActive(BreakIndicators.class) || Modules.get().get(NoRender.class).noBlockBreakOverlay()) info.cancel();
    }
}
