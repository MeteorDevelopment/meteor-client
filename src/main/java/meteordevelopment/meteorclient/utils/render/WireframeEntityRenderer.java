/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.mixininterface.IMultiPhase;
import meteordevelopment.meteorclient.mixininterface.IMultiPhaseParameters;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class WireframeEntityRenderer {
    private static final MatrixStack matrices = new MatrixStack();

    private static Renderer3D renderer;

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

        float tickDelta = mc.world.getTickManager().isFrozen() ? 1 : event.tickDelta;

        offsetX = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
        offsetY = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
        offsetZ = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

        var renderer = (EntityRenderer<Entity, EntityRenderState>) mc.getEntityRenderDispatcher().getRenderer(entity);
        var state = renderer.getAndUpdateRenderState(entity, tickDelta);

        Vec3d entityOffset = renderer.getPositionOffset(state);
        offsetX += entityOffset.x;
        offsetY += entityOffset.y;
        offsetZ += entityOffset.z;

        matrices.push();
        matrices.scale((float) scale, (float) scale, (float) scale);
        renderer.render(state, matrices, MyVertexConsumerProvider.INSTANCE, 15);
        matrices.pop();
    }

    private static class MyVertexConsumerProvider implements VertexConsumerProvider {
        public static final MyVertexConsumerProvider INSTANCE = new MyVertexConsumerProvider();
        private final Object2ObjectOpenHashMap<RenderLayer, MyVertexConsumer> buffers = new Object2ObjectOpenHashMap<>();

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            //noinspection ConstantValue
            if (layer instanceof IMultiPhase phase && ((IMultiPhaseParameters) (Object) phase.meteor$getParameters()).meteor$getTarget() == RenderPhase.ITEM_ENTITY_TARGET) {
                return NoopVertexConsumer.INSTANCE;
            }

            MyVertexConsumer vertexConsumer = buffers.get(layer);

            if (vertexConsumer == null) {
                vertexConsumer = new MyVertexConsumer();
                buffers.put(layer, vertexConsumer);
            }

            return vertexConsumer;
        }
    }

    private static class MyVertexConsumer implements VertexConsumer {
        private final float[] xs = new float[4];
        private final float[] ys = new float[4];
        private final float[] zs = new float[4];

        private int i = 0;

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
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
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }
    }

    private static class NoopVertexConsumer implements VertexConsumer {
        private static final NoopVertexConsumer INSTANCE = new NoopVertexConsumer();

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }
    }
}
