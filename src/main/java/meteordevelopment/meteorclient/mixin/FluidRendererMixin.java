/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public abstract class FluidRendererMixin {
    @Unique private final ThreadLocal<Integer> alphas = ThreadLocal.withInitial(() -> -1);
    @Unique private final ThreadLocal<Boolean> ambient = ThreadLocal.withInitial(() -> false);

    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockAndTintGetter world, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo info) {
        if (Modules.get() == null) return;

        Ambience ambience = Modules.get().get(Ambience.class);
        ambient.set(ambience.isActive() && ambience.customLavaColor.get() && fluidState.is(FluidTags.LAVA));

        int alpha = Xray.getAlpha(fluidState.createLegacyBlock(), pos);
        if (alpha == 0) info.cancel();
        else alphas.set(alpha);
    }

    @ModifyExpressionValue(
        method = "tesselate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/FluidModel;layer()Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"
        )
    )
    private ChunkSectionLayer modifyLayer(ChunkSectionLayer layer) {
        if (ambient.get()) {
            int lavaAlpha = Modules.get().get(Ambience.class).lavaColor.get().a;
            if (lavaAlpha > 0 && lavaAlpha < 255) return ChunkSectionLayer.TRANSLUCENT;
        }

        int alpha = alphas.get();
        if (alpha > 0 && alpha < 255) return ChunkSectionLayer.TRANSLUCENT;
        return layer;
    }

    @Inject(method = "vertex", at = @At("HEAD"), cancellable = true)
    private void onVertex(VertexConsumer vertexConsumer, float x, float y, float z, int color, float u, float v, int light, CallbackInfo info) {
        int alpha = alphas.get();

        if (ambient.get()) {
            Color lavaColor = Modules.get().get(Ambience.class).lavaColor.get();
            int tintedColor = ARGB.color(alpha != -1 ? alpha : lavaColor.a, lavaColor.r, lavaColor.g, lavaColor.b);
            vertexConsumer.addVertex(x, y, z, tintedColor, u, v, OverlayTexture.NO_OVERLAY, light, 0.0f, 1.0f, 0.0f);
            info.cancel();
        }
        else if (alpha != -1) {
            int tintedColor = ARGB.color(alpha, (color >> 16) & 255, (color >> 8) & 255, color & 255);
            vertexConsumer.addVertex(x, y, z, tintedColor, u, v, OverlayTexture.NO_OVERLAY, light, 0.0f, 1.0f, 0.0f);
            info.cancel();
        }
    }
}
