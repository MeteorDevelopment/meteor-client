/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MeshRenderer {
    private static final MeshRenderer INSTANCE = new MeshRenderer();

    private static boolean taken;

    private GpuTextureView colorAttachment;
    private GpuTextureView depthAttachment;
    private @Nullable Color clearColor;
    private RenderPipeline pipeline;
    private @Nullable MeshBuilder mesh;
    private @Nullable GpuBufferSlice vertexBuffer;
    private @Nullable GpuBufferSlice indexBuffer;
    private IndexType indexType = IndexType.INT;
    private Matrix4f matrix;
    private final Map<String, GpuBufferSlice> uniforms = new Object2ObjectOpenHashMap<>();
    private final Map<String, TextureViewAndSampler> samplers = new Object2ObjectOpenHashMap<>();

    private record TextureViewAndSampler(GpuTextureView textureView, GpuSampler sampler) {
    }

    private MeshRenderer() {
    }

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

    public MeshRenderer attachments(RenderTarget framebuffer) {
        colorAttachment = framebuffer.getColorTextureView();
        depthAttachment = framebuffer.getDepthTextureView();
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

    public MeshRenderer mesh(GpuBufferSlice vertices, GpuBufferSlice indices) {
        return mesh(vertices, indices, IndexType.INT);
    }

    public MeshRenderer mesh(GpuBufferSlice vertices, GpuBufferSlice indices, IndexType indexType) {
        this.vertexBuffer = vertices;
        this.indexBuffer = indices;
        this.indexType = indexType;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh) {
        this.mesh = mesh;
        this.indexType = mesh.getIndexType();
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, Matrix4f matrix) {
        this.mesh = mesh;
        return this.transform(matrix);
    }

    public MeshRenderer mesh(MeshBuilder mesh, PoseStack matrices) {
        this.mesh = mesh;
        return this.transform(matrices);
    }

    public MeshRenderer transform(Matrix4f matrix) {
        this.matrix = matrix;
        return this;
    }

    public MeshRenderer transform(PoseStack matrices) {
        this.matrix = matrices.last().pose();
        return this;
    }

    public MeshRenderer fullscreen() {
        return this.mesh(FullScreenRenderer.vbo.slice(), FullScreenRenderer.ibo.slice());
    }

    public MeshRenderer uniform(String name, GpuBufferSlice slice) {
        uniforms.put(name, slice);
        return this;
    }

    public MeshRenderer sampler(String name, GpuTextureView view, GpuSampler sampler) {
        if (name != null && view != null && sampler != null) {
            samplers.put(name, new TextureViewAndSampler(view, sampler));
        }

        return this;
    }

    public void end() {
        if (mesh != null && mesh.isBuilding()) {
            mesh.end();
        }

        int indexCount = mesh != null ? mesh.getIndicesCount()
            : (int) (indexBuffer != null ? indexBuffer.length() / indexType.bytes : -1);
        // todo hope this is alright @minegame take a look please (lossy conversion from long to int)

        if (indexCount > 0) {

            if (Utils.rendering3D || matrix != null) {
                RenderSystem.getModelViewStack().pushMatrix();
            }

            if (matrix != null) {
                RenderSystem.getModelViewStack().mul(matrix);
            }

            if (Utils.rendering3D) {
                applyCameraPos();
            }

            Optional<Vector4fc> clearColor = this.clearColor != null
                ? Optional.of(this.clearColor.getVec4f())
                : Optional.empty();

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            GpuBufferSlice vertexBuffer = mesh != null ? mesh.uploadVertexBuffer(encoder) : Objects.requireNonNull(this.vertexBuffer);
            GpuBufferSlice indexBuffer = mesh != null ? mesh.uploadIndexBuffer(encoder) : Objects.requireNonNull(this.indexBuffer);
            int firstIndex = getFirstIndex(indexBuffer);

            GpuBufferSlice meshData = MeshUniforms.write(RenderUtils.projection, RenderSystem.getModelViewStack());

            try (RenderPass pass = (depthAttachment != null && pipeline.wantsDepthTexture()) ?
                encoder.createRenderPass(() -> "Meteor MeshRenderer", colorAttachment, clearColor, depthAttachment, OptionalDouble.empty()) :
                encoder.createRenderPass(() -> "Meteor MeshRenderer", colorAttachment, clearColor)) {

                pass.setPipeline(pipeline);
                pass.setUniform("MeshData", meshData);

                for (var entry : uniforms.entrySet()) {
                    pass.setUniform(entry.getKey(), entry.getValue());
                }

                for (var entry : samplers.entrySet()) {
                    pass.bindTexture(entry.getKey(), entry.getValue().textureView, entry.getValue().sampler);
                }

                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(indexBuffer.buffer(), indexType);
                pass.drawIndexed(indexCount, 1, firstIndex, 0, 0);
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
        vertexBuffer = null;
        indexBuffer = null;
        indexType = IndexType.INT;
        matrix = null;
        uniforms.clear();
        samplers.clear();

        taken = false;
    }

    private int getFirstIndex(GpuBufferSlice indexBuffer) {
        if (indexBuffer.offset() % indexType.bytes != 0) {
            throw new IllegalArgumentException("Index buffer offset must be aligned to " + indexType.bytes + " bytes.");
        }

        return Math.toIntExact(indexBuffer.offset() / indexType.bytes);
    }

    private static void applyCameraPos() {
        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
        RenderSystem.getModelViewStack().translate(0, (float) -cameraPos.y, 0);
    }
}
