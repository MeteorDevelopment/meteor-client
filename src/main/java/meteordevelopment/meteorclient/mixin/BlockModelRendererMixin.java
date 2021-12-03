/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.WallHack;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {
    @Inject(method = "renderQuad(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;FFFFIIIII)V",
        at = @At("TAIL"))
    private void onRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, MatrixStack.Entry matrixEntry, BakedQuad quad, float brightness0, float brightness1, float brightness2, float brightness3, int light0, int light1, int light2, int light3, int overlay, CallbackInfo ci) {
        WallHack wallHack = Modules.get().get(WallHack.class);
        Xray xray = Modules.get().get(Xray.class);

        if (wallHack.isActive() && wallHack.blocks.get().contains(state.getBlock())) {
            int alpha;

            if (xray.isActive()) {
                alpha = xray.opacity.get();
            } else {
                alpha = wallHack.opacity.get();
            }

            rewriteBuffer(vertexConsumer, alpha);
        } else if (xray.isActive() && !wallHack.isActive() && xray.isBlocked(state.getBlock(), pos)) {
            rewriteBuffer(vertexConsumer, xray.opacity.get());
        }
    }

    private void rewriteBuffer(VertexConsumer vertexConsumer, int alpha) {
        if (vertexConsumer instanceof BufferBuilder bufferBuilder) {
            BufferBuilderAccessor bufferBuilderAccessor = ((BufferBuilderAccessor) bufferBuilder);

            int prevOffset = bufferBuilderAccessor.getElementOffset();

            if (prevOffset > 0) {
                int i = bufferBuilderAccessor.getVertexFormat().getVertexSize();

                for (int l = 1; l <= 4; l++) {
                    bufferBuilderAccessor.setElementOffset(prevOffset - i * l);
                    bufferBuilder.putByte(15, (byte) (alpha));
                }

                bufferBuilderAccessor.setElementOffset(prevOffset);
            }
        }
    }
}
