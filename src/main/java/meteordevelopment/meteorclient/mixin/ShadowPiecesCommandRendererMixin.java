/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.ShadowPiecesCommandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowPiecesCommandRenderer.class)
public abstract class ShadowPiecesCommandRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void meteor$onRender(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers, CallbackInfo info) {
        if (queue.getShadowPiecesCommands().isEmpty()) {
            info.cancel();
        }
    }
}
