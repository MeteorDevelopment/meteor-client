/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.mixininterface.IBakedQuad;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SimpleBlockRenderer {
    private static final MatrixStack MATRICES = new MatrixStack();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Random RANDOM = Random.create();

    private SimpleBlockRenderer() {
    }

    public static void renderWithBlockEntity(BlockEntity blockEntity, float tickDelta, IVertexConsumerProvider vertexConsumerProvider) {
        vertexConsumerProvider.setOffset(blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ());
        SimpleBlockRenderer.render(blockEntity.getPos(), blockEntity.getCachedState(), vertexConsumerProvider);

        BlockEntityRenderer<BlockEntity> renderer = mc.getBlockEntityRenderDispatcher().get(blockEntity);
        if (renderer != null && blockEntity.hasWorld() && blockEntity.getType().supports(blockEntity.getCachedState())) renderer.render(blockEntity, tickDelta, MATRICES, vertexConsumerProvider, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        vertexConsumerProvider.setOffset(0, 0, 0);
    }

    public static void render(BlockPos pos, BlockState state, VertexConsumerProvider consumerProvider) {
        if (state.getRenderType() != BlockRenderType.MODEL) return;

        VertexConsumer consumer = consumerProvider.getBuffer(RenderLayer.getSolid());
        BakedModel model = mc.getBlockRenderManager().getModel(state);
        Vec3d offset = state.getModelOffset(mc.world, pos);

        float offsetX = (float) offset.x;
        float offsetY = (float) offset.y;
        float offsetZ = (float) offset.z;

        for (Direction direction : DIRECTIONS) {
            List<BakedQuad> list = model.getQuads(state, direction, RANDOM);
            if (!list.isEmpty()) renderQuads(list, offsetX, offsetY, offsetZ, consumer);
        }

        List<BakedQuad> list = model.getQuads(state, null, RANDOM);
        if (!list.isEmpty()) renderQuads(list, offsetX, offsetY, offsetZ, consumer);
    }

    private static void renderQuads(List<BakedQuad> quads, float offsetX, float offsetY, float offsetZ, VertexConsumer consumer) {
        for (BakedQuad bakedQuad : quads) {
            IBakedQuad quad = (IBakedQuad) bakedQuad;

            for (int j = 0; j < 4; j++) {
                float x = quad.meteor$getX(j);
                float y = quad.meteor$getY(j);
                float z = quad.meteor$getZ(j);

                consumer.vertex(offsetX + x, offsetY + y, offsetZ + z);
            }
        }
    }
}
