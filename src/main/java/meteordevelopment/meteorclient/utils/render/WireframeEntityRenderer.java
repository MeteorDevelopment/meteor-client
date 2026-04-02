/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixin.RenderTypeAccessor;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WireframeEntityRenderer {
    private static final PoseStack matrices = new PoseStack();

    private static Renderer3D renderer;

    private static final SubmitNodeStorage renderCommandQueue = new SubmitNodeStorage();

    private static final FeatureRenderDispatcher renderDispatcher = new FeatureRenderDispatcher(
        renderCommandQueue,
        mc.getBlockRenderer(),
        MyVertexConsumerProvider.INSTANCE,
        mc.getAtlasManager(),
        NoopOutlineVertexConsumerProvider.INSTANCE,
        NoopImmediateVertexConsumerProvider.INSTANCE,
        mc.font
    );

    private static Color sideColor;
    private static Color lineColor;
    private static ShapeMode shapeMode;

    private static double offsetX;
    private static double offsetY;
    private static double offsetZ;

    private WireframeEntityRenderer() {
    }

    @SuppressWarnings("unchecked")
    public static void render(Render3DEvent event, Entity entity, double scale, Color sideColor, Color lineColor, ShapeMode shapeMode) {
        WireframeEntityRenderer.renderer = event.renderer;
        WireframeEntityRenderer.sideColor = sideColor;
        WireframeEntityRenderer.lineColor = lineColor;
        WireframeEntityRenderer.shapeMode = shapeMode;

        float tickDelta = mc.level.tickRateManager().isFrozen() ? 1 : event.tickDelta;

        offsetX = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        offsetY = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        offsetZ = Mth.lerp(tickDelta, entity.zOld, entity.getZ());

        var renderer = (EntityRenderer<Entity, EntityRenderState>) mc.getEntityRenderDispatcher().getRenderer(entity);
        var state = renderer.createRenderState(entity, tickDelta);

        Vec3 entityOffset = renderer.getRenderOffset(state);
        offsetX += entityOffset.x;
        offsetY += entityOffset.y;
        offsetZ += entityOffset.z;

        matrices.pushPose();
        matrices.scale((float) scale, (float) scale, (float) scale);
        renderer.submit(state, matrices, renderCommandQueue, mc.gameRenderer.getLevelRenderState().cameraRenderState);
        matrices.popPose();

        renderDispatcher.renderAllFeatures();
        renderCommandQueue.endFrame();
    }

    private static class MyVertexConsumerProvider extends MultiBufferSource.BufferSource {
        public static final MyVertexConsumerProvider INSTANCE = new MyVertexConsumerProvider();
        private final Object2ObjectOpenHashMap<RenderType, MyVertexConsumer> buffers = new Object2ObjectOpenHashMap<>();

        protected MyVertexConsumerProvider() {
            super(null, null);
        }

        @Override
        public VertexConsumer getBuffer(RenderType layer) {
            if (((RenderTypeAccessor) layer).getState().outputTarget == OutputTarget.ITEM_ENTITY_TARGET) {
                return NoopVertexConsumer.INSTANCE;
            }

            MyVertexConsumer vertexConsumer = buffers.get(layer);

            if (vertexConsumer == null) {
                vertexConsumer = new MyVertexConsumer();
                buffers.put(layer, vertexConsumer);
            }

            return vertexConsumer;
        }

        @Override
        public void endBatch() {
            throw new RuntimeException();
        }

        @Override
        public void endBatch(RenderType layer) {
            throw new RuntimeException();
        }
    }

    private static class MyVertexConsumer implements VertexConsumer {
        private final float[] xs = new float[4];
        private final float[] ys = new float[4];
        private final float[] zs = new float[4];

        private int i = 0;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            xs[i] = x;
            ys[i] = y;
            zs[i] = z;

            i++;

            if (i == 4) {
                renderer.side(
                    offsetX + xs[0], offsetY + ys[0], offsetZ + zs[0],
                    offsetX + xs[1], offsetY + ys[1], offsetZ + zs[1],
                    offsetX + xs[2], offsetY + ys[2], offsetZ + zs[2],
                    offsetX + xs[3], offsetY + ys[3], offsetZ + zs[3],
                    sideColor,
                    lineColor,
                    shapeMode
                );

                i = 0;
            }

            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
