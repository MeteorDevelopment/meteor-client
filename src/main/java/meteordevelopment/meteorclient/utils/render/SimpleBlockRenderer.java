/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public abstract class SimpleBlockRenderer {
    private static final PoseStack MATRICES = new MatrixStack();
    private static final List<BlockModelPart> PARTS = new ArrayList<>();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final RandomSource RANDOM = RandomSource.create();

    private static final SubmitNodeStorage renderCommandQueue = new OrderedRenderCommandQueueImpl();

    private static MultiBufferSource provider;

    private static final FeatureRenderDispatcher renderDispatcher = new RenderDispatcher(
        renderCommandQueue,
        mc.getBlockRenderManager(),
        new WrapperImmediateVertexConsumerProvider(() -> provider),
        mc.getAtlasManager(),
        NoopOutlineVertexConsumerProvider.INSTANCE,
        NoopImmediateVertexConsumerProvider.INSTANCE,
        mc.textRenderer
    );

    private SimpleBlockRenderer() {
    }

    public static void renderWithBlockEntity(BlockEntity blockEntity, float tickDelta, IVertexConsumerProvider vertexConsumerProvider) {
        vertexConsumerProvider.setOffset(blockEntity.getPos().getX(), blockEntity.getPos().getY(), blockEntity.getPos().getZ());
        SimpleBlockRenderer.render(blockEntity.getPos(), blockEntity.getCachedState(), vertexConsumerProvider);

        BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = mc.getBlockEntityRenderDispatcher().get(blockEntity);

        if (renderer != null && blockEntity.hasWorld() && blockEntity.getType().supports(blockEntity.getCachedState())) {
            SimpleBlockRenderer.provider = vertexConsumerProvider;

            BlockEntityRenderState state = renderer.createRenderState();
            renderer.updateRenderState(blockEntity, state, tickDelta, mc.gameRenderer.getCamera().getCameraPos(), null);
            renderer.render(state, MATRICES, renderCommandQueue, mc.gameRenderer.getEntityRenderStates().cameraRenderState);

            renderDispatcher.render();
            renderCommandQueue.onNextFrame();

            SimpleBlockRenderer.provider = null;
        }

        vertexConsumerProvider.setOffset(0, 0, 0);
    }

    public static void render(BlockPos pos, BlockState state, MultiBufferSource consumerProvider) {
        if (state.getRenderType() != RenderShape.MODEL) return;

        VertexConsumer consumer = consumerProvider.getBuffer(RenderTypes.solid());

        BlockStateModel model = mc.getBlockRenderManager().getModel(state);
        model.addParts(RANDOM, PARTS);

        Vec3 offset = state.getModelOffset(pos);
        float offsetX = (float) offset.x;
        float offsetY = (float) offset.y;
        float offsetZ = (float) offset.z;

        for (BlockModelPart part : PARTS) {
            for (Direction direction : DIRECTIONS) {
                List<BakedQuad> quads = part.getQuads(direction);
                if (!quads.isEmpty()) renderQuads(quads, offsetX, offsetY, offsetZ, consumer);
            }

            List<BakedQuad> quads = part.getQuads(null);
            if (!quads.isEmpty()) renderQuads(quads, offsetX, offsetY, offsetZ, consumer);
        }

        PARTS.clear();
    }

    private static void renderQuads(List<BakedQuad> quads, float offsetX, float offsetY, float offsetZ, VertexConsumer consumer) {
        for (BakedQuad quad : quads) {
            for (int j = 0; j < 4; j++) {
                Vector3fc vec = quad.getPosition(j);
                consumer.vertex(offsetX + vec.x(), offsetY + vec.y(), offsetZ + vec.z());
            }
        }
    }
}
