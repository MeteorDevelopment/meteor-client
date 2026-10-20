/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@NullMarked
public abstract class SimpleBlockRenderer {
    private static final PoseStack MATRICES = new PoseStack();
    private static final List<BlockStateModelPart> PARTS = new ArrayList<>();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final RandomSource RANDOM = RandomSource.create();

    private static final ScopedValue<VertexConsumer> CONSUMER = ScopedValue.newInstance();

    private static final SubmitNodeStorage SUBMIT_NODES = new SubmitNodeStorage() {
        @Override
        public SubmitNodeCollection order(int i) {
            return MESH_NODES;
        }
    };

    private static final SubmitNodeCollection MESH_NODES = new SubmitNodeCollection() {
        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
            if (CONSUMER.isBound()) {
                model.setupAnim(state);
                model.renderToBuffer(poseStack, CONSUMER.get(), lightCoords, overlayCoords, tintedColor);
            }
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        }
    };

    private SimpleBlockRenderer() {
    }

    public static void renderWithBlockEntity(BlockEntity blockEntity, float tickDelta, IVertexConsumerProvider vertexConsumerProvider) {
        vertexConsumerProvider.setOffset(blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ());
        SimpleBlockRenderer.render(blockEntity.getBlockPos(), blockEntity.getBlockState(), vertexConsumerProvider);

        BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(blockEntity);

        if (renderer != null && blockEntity.hasLevel() && blockEntity.getType().isValid(blockEntity.getBlockState())) {
            BlockEntityRenderState state = renderer.createRenderState();
            renderer.extractRenderState(blockEntity, state, tickDelta, mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos, null);

            ScopedValue.where(CONSUMER, vertexConsumerProvider.getBuffer(RenderTypes.solidMovingBlock()))
                .run(() -> renderer.submit(state, MATRICES, SUBMIT_NODES, mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState));
        }

        vertexConsumerProvider.setOffset(0, 0, 0);
    }

    public static void render(BlockPos pos, BlockState state, IVertexConsumerProvider consumerProvider) {
        if (state.getRenderShape() != RenderShape.MODEL) return;

        VertexConsumer consumer = consumerProvider.getBuffer(RenderTypes.solidMovingBlock());

        BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(state);
        model.collectParts(RANDOM, PARTS);

        Vec3 offset = state.getOffset(pos);
        float offsetX = (float) offset.x;
        float offsetY = (float) offset.y;
        float offsetZ = (float) offset.z;

        for (BlockStateModelPart part : PARTS) {
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
                Vector3fc vec = quad.position(j);
                consumer.addVertex(offsetX + vec.x(), offsetY + vec.y(), offsetZ + vec.z());
            }
        }
    }
}
