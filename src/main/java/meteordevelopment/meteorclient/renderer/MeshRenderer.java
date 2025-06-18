/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeshRenderer {
    private static final MeshRenderer INSTANCE = new MeshRenderer();

    private static boolean taken;

    private GpuTextureView colorAttachment;
    private GpuTextureView depthAttachment;
    private Color clearColor;
    private RenderPipeline pipeline;
    private MeshBuilder mesh;
    private Matrix4f matrix;
    private final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final HashMap<String, GpuTextureView> samplers = new HashMap<>();

    private MeshRenderer() {}

    public static MeshRenderer begin() {
        if (taken)
            throw new IllegalStateException("Previous instance of MeshRenderer was not ended");

        taken = true;
        return INSTANCE;
    }

    public MeshRenderer attachments(GpuTextureView color, GpuTextureView depth) {
        colorAttachment = color;
        depthAttachment = depth;
        return this;
    }

    public MeshRenderer attachments(Framebuffer framebuffer) {
        colorAttachment = framebuffer.getColorAttachmentView();
        depthAttachment = framebuffer.getDepthAttachmentView();
        return this;
    }

    public MeshRenderer clearColor(Color color) {
        clearColor = color;
        return this;
    }

    public MeshRenderer pipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh) {
        this.mesh = mesh;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, Matrix4f matrix) {
        this.mesh = mesh;
        this.matrix = matrix;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, MatrixStack matrices) {
        this.mesh = mesh;
        this.matrix = matrices.peek().getPositionMatrix();
        return this;
    }

    public MeshRenderer uniform(String name, GpuBufferSlice slice) {
        uniforms.put(name, slice);
        return this;
    }

    public MeshRenderer sampler(String name, GpuTextureView view) {
        if (name != null && view != null) {
            samplers.put(name, view);
        }

        return this;
    }

    public void end() {
        if (mesh.isBuilding()) {
            mesh.end();
        }

        if (mesh.getIndicesCount() > 0) {
            if (Utils.rendering3D || matrix != null) {
                RenderSystem.getModelViewStack().pushMatrix();
            }

            if (matrix != null) {
                RenderSystem.getModelViewStack().mul(matrix);
            }

            if (Utils.rendering3D) {
                applyCameraPos();
            }

            GpuBuffer vertexBuffer = mesh.getVertexBuffer();
            GpuBuffer indexBuffer = mesh.getIndexBuffer();

            {
                OptionalInt clearColor = this.clearColor != null ?
                    OptionalInt.of(ColorHelper.getArgb(this.clearColor.a, this.clearColor.r, this.clearColor.g, this.clearColor.b)) :
                    OptionalInt.empty();

                GpuBufferSlice meshData = MeshUniforms.write(RenderUtils.projection, RenderSystem.getModelViewStack());

                RenderPass pass = (depthAttachment != null && pipeline.wantsDepthTexture()) ?
                    RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Meteor MeshRenderer", colorAttachment, clearColor, depthAttachment, OptionalDouble.empty()) :
                    RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Meteor MeshRenderer", colorAttachment, clearColor);

                pass.setPipeline(pipeline);
                pass.setUniform("MeshData", meshData);

                for (var name : uniforms.keySet()) {
                    pass.setUniform(name, uniforms.get(name));
                }

                for (var name : samplers.keySet()) {
                    pass.bindSampler(name, samplers.get(name));
                }

                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(indexBuffer, VertexFormat.IndexType.INT);
                pass.drawIndexed(0, 0, mesh.getIndicesCount(), 1);

                pass.close();
            }

            if (Utils.rendering3D || matrix != null) {
                RenderSystem.getModelViewStack().popMatrix();
            }
        }

        colorAttachment = null;
        depthAttachment = null;
        clearColor = null;
        pipeline = null;
        mesh = null;
        matrix = null;
        uniforms.clear();
        samplers.clear();

        taken = false;
    }

    private static void applyCameraPos() {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        RenderSystem.getModelViewStack().translate(0, (float) -cameraPos.y, 0);
    }
}
