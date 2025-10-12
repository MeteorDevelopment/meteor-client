/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

public class OutlineRenderCommandQueue extends OrderedRenderCommandQueueImpl {
    private int color;
    private int[] tints;

    public void setColor(Color color) {
        this.color = color.getPacked();
    }

    @Override
    public BatchingRenderCommandQueue getBatchingQueue(int i) {
        return batchingQueues.computeIfAbsent(i, order -> new OutlineBatchingRenderCommandQueue(this));
    }

    private class OutlineBatchingRenderCommandQueue extends BatchingRenderCommandQueue {
        public OutlineBatchingRenderCommandQueue(OrderedRenderCommandQueueImpl orderedQueueImpl) {
            super(orderedQueueImpl);
        }

        @Override
        public void submitDebugHitbox(MatrixStack matrices, EntityRenderState renderState, EntityHitboxAndView debugHitbox) {
        }

        @Override
        public void submitShadowPieces(MatrixStack matrices, float shadowRadius, List<EntityRenderState.ShadowPiece> shadowPieces) {
        }

        @Override
        public void submitLabel(MatrixStack matrices, @Nullable Vec3d nameLabelPos, int y, Text label, boolean notSneaking, int light, double squaredDistanceToCamera, CameraRenderState cameraState) {
        }

        @Override
        public void submitText(MatrixStack matrices, float x, float y, OrderedText text, boolean dropShadow, TextRenderer.TextLayerType layerType, int light, int color, int backgroundColor, int outlineColor) {
        }

        @Override
        public void submitFire(MatrixStack matrices, EntityRenderState renderState, Quaternionf rotation) {
        }

        @Override
        public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leashData) {
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, MatrixStack matrices, RenderLayer renderLayer, int light, int overlay, int tintedColor, @Nullable Sprite sprite, int outlineColor, @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
            super.submitModel(model, state, matrices, renderLayer, light, overlay, color, sprite, 0, crumblingOverlay);
        }

        @Override
        public void submitModelPart(ModelPart part, MatrixStack matrices, RenderLayer renderLayer, int light, int overlay, @Nullable Sprite sprite, boolean sheeted, boolean hasGlint, int tintedColor, @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay, int i) {
            super.submitModelPart(part, matrices, renderLayer, light, overlay, sprite, sheeted, hasGlint, color, crumblingOverlay, i);
        }

        @Override
        public void submitBlock(MatrixStack matrices, BlockState state, int light, int overlay, int outlineColor) {
        }

        @Override
        public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
        }

        @Override
        public void submitBlockStateModel(MatrixStack matrices, RenderLayer renderLayer, BlockStateModel model, float r, float g, float b, int light, int overlay, int outlineColor) {
            r = Color.toRGBAR(color) / 255f;
            g = Color.toRGBAG(color) / 255f;
            b = Color.toRGBAB(color) / 255f;

            super.submitBlockStateModel(matrices, renderLayer, model, r, g, b, light, overlay, outlineColor);
        }

        @Override
        public void submitItem(MatrixStack matrices, ItemDisplayContext displayContext, int light, int overlay, int outlineColors, int[] tintLayers, List<BakedQuad> quads, RenderLayer renderLayer, ItemRenderState.Glint glintType) {
            if (tints == null || tints[0] != color) {
                tints = new int[] { color, color, color, color };
            }

            super.submitItem(matrices, displayContext, light, overlay, outlineColors, tints, quads, renderLayer, glintType);
        }

        @Override
        public void submitCustom(MatrixStack matrices, RenderLayer renderLayer, Custom customRenderer) {
        }

        @Override
        public void submitCustom(LayeredCustom customRenderer) {
        }
    }
}
