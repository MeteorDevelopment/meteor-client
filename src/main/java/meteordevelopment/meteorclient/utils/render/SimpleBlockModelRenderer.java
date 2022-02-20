/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2022 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SimpleBlockModelRenderer {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Random RANDOM = new Random();

    public static void render(BlockPos pos, BlockState state, VertexConsumerProvider consumerProvider) {
        if (state.getRenderType() != BlockRenderType.MODEL) return;

        VertexConsumer consumer = consumerProvider.getBuffer(RenderLayer.getSolid());
        BakedModel model = mc.getBlockRenderManager().getModel(state);
        Vec3d offset = state.getModelOffset(mc.world, pos);

        double offsetX = pos.getX() + offset.x;
        double offsetY = pos.getY() + offset.y;
        double offsetZ = pos.getZ() + offset.z;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < DIRECTIONS.length; i++) {
            List<BakedQuad> list = model.getQuads(state, DIRECTIONS[i], RANDOM);
            if (!list.isEmpty()) renderQuads(list, offsetX, offsetY, offsetZ, consumer);
        }

        List<BakedQuad> list = model.getQuads(state, null, RANDOM);
        if (!list.isEmpty()) renderQuads(list, offsetX, offsetY, offsetZ, consumer);
    }

    private static void renderQuads(List<BakedQuad> quads, double offsetX, double offsetY, double offsetZ, VertexConsumer consumer) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < quads.size(); i++) {
            int[] vertexData = quads.get(i).getVertexData();

            for (int j = 0; j < 4; j++) {
                float x = Float.intBitsToFloat(vertexData[j * 8]);
                float y = Float.intBitsToFloat(vertexData[j * 8 + 1]);
                float z = Float.intBitsToFloat(vertexData[j * 8 + 2]);

                consumer.vertex(offsetX + x, offsetY + y, offsetZ + z).next();
            }
        }
    }
}
