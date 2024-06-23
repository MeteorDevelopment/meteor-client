/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin {
    @Unique private final ThreadLocal<Integer> alphas = new ThreadLocal<>();

    @Inject(method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay, CallbackInfo info) {
        int alpha = Xray.getAlpha(state, pos);

        if (alpha == 0) info.cancel();
        else alphas.set(alpha);
    }

    @Inject(method = "renderQuad(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;FFFFIIIII)V", at = @At("TAIL"))
    private void onRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, MatrixStack.Entry matrixEntry, BakedQuad quad, float brightness0, float brightness1, float brightness2, float brightness3, int light0, int light1, int light2, int light3, int overlay, CallbackInfo ci) {
        int alpha = alphas.get();
        if (alpha != -1) rewriteBuffer(vertexConsumer, alpha);
    }

    @Unique
    private void rewriteBuffer(VertexConsumer vertexConsumer, int alpha) {
        if (vertexConsumer instanceof BufferBuilderAccessor bufferBuilder) {
            int i = bufferBuilder.getVertexFormat().getVertexSizeByte();
            try (BufferAllocator.CloseableBuffer allocatedBuffer = bufferBuilder.meteor$getAllocator().getAllocated()) {
                if (allocatedBuffer != null) {
                    ByteBuffer buffer = allocatedBuffer.getBuffer();
                    for (int l = 1; l <= 4; l++) {
                        buffer.put(buffer.capacity() - i * l + 15, (byte) (alpha));
                    }
                }
            }
        }
    }
}
