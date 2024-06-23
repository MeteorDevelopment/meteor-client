/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.renderer.Mesh;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

public class MeshVertexConsumerProvider implements IVertexConsumerProvider {
    private final MeshVertexConsumer vertexConsumer;

    public MeshVertexConsumerProvider(Mesh mesh) {
        vertexConsumer = new MeshVertexConsumer(mesh);
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return vertexConsumer;
    }

    public void setColor(Color color) {
        vertexConsumer.fixedColor(color.r, color.g, color.b, color.a);
    }

    @Override
    public void setOffset(float offsetX, float offsetY, float offsetZ) {
        vertexConsumer.setOffset(offsetX, offsetY, offsetZ);
    }

    public static class MeshVertexConsumer implements VertexConsumer {
        private final Mesh mesh;

        private float offsetX, offsetY, offsetZ;

        private final float[] xs = new float[4];
        private final float[] ys = new float[4];
        private final float[] zs = new float[4];
        private final Color color = new Color();

        private int i;

        public MeshVertexConsumer(Mesh mesh) {
            this.mesh = mesh;
        }

        public void setOffset(float offsetX, float offsetY, float offsetZ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            xs[i] = offsetX + x;
            ys[i] = offsetY + y;
            zs[i] = offsetZ + z;

            if (++i >= 4) {
                mesh.quad(
                    mesh.vec3(xs[0], ys[0], zs[0]).color(color).next(),
                    mesh.vec3(xs[1], ys[1], zs[1]).color(color).next(),
                    mesh.vec3(xs[2], ys[2], zs[2]).color(color).next(),
                    mesh.vec3(xs[3], ys[3], zs[3]).color(color).next()
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
            return null;
        }

        public void fixedColor(int red, int green, int blue, int alpha) {
            color.set(red, green, blue, alpha);
        }
    }
}
