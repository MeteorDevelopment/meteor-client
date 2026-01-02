/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

public class MeshBuilderVertexConsumerProvider implements IVertexConsumerProvider {
    private final MeshBuilderVertexConsumer vertexConsumer;

    public MeshBuilderVertexConsumerProvider(MeshBuilder mesh) {
        vertexConsumer = new MeshBuilderVertexConsumer(mesh);
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        return new W(vertexConsumer); // new instance each call to fix duplicate delegates
    }

    private record W(MeshBuilderVertexConsumer d) implements VertexConsumer {
        public VertexConsumer vertex(float x, float y, float z) {
            d.vertex(x, y, z);
            return this;
        }

        public VertexConsumer color(int r, int g, int b, int a) {
            return this;
        }

        public VertexConsumer color(int c) {
            return this;
        }

        public VertexConsumer texture(float u, float v) {
            return this;
        }

        public VertexConsumer overlay(int u, int v) {
            return this;
        }

        public VertexConsumer light(int u, int v) {
            return this;
        }

        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        public VertexConsumer lineWidth(float w) {
            return this;
        }
    }

    public void setColor(Color color) {
        vertexConsumer.fixedColor(color.r, color.g, color.b, color.a);
    }

    @Override
    public void setOffset(int offsetX, int offsetY, int offsetZ) {
        vertexConsumer.setOffset(offsetX, offsetY, offsetZ);
    }

    public static class MeshBuilderVertexConsumer implements VertexConsumer {
        private final MeshBuilder mesh;

        private int offsetX, offsetY, offsetZ;

        private final double[] xs = new double[4];
        private final double[] ys = new double[4];
        private final double[] zs = new double[4];
        private final Color color = new Color();

        private int i;

        public MeshBuilderVertexConsumer(MeshBuilder mesh) {
            this.mesh = mesh;
        }

        public void setOffset(int offsetX, int offsetY, int offsetZ) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            xs[i] = (double) offsetX + x;
            ys[i] = (double) offsetY + y;
            zs[i] = (double) offsetZ + z;

            if (++i >= 4) {
                mesh.ensureQuadCapacity();

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
        public VertexConsumer color(int argb) {
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

        @Override
        public VertexConsumer lineWidth(float width) {
            return this;
        }

        public void fixedColor(int red, int green, int blue, int alpha) {
            color.set(red, green, blue, alpha);
        }
    }
}
