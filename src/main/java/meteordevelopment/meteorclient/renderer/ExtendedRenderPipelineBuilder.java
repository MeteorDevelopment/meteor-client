/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.mixininterface.IRenderPipeline;
import net.minecraft.resources.Identifier;

public class ExtendedRenderPipelineBuilder {
    private final RenderPipeline.Builder builder;
    private boolean lineSmooth;
    private CompareOp depthTest;
    private Boolean writeDepth;
    private BlendFunction blendFunction;

    public ExtendedRenderPipelineBuilder(RenderPipeline.Snippet... snippets) {
        builder = RenderPipeline.builder(snippets);
    }

    public ExtendedRenderPipelineBuilder withLocation(Identifier location) {
        builder.withLocation(location);
        return this;
    }

    public ExtendedRenderPipelineBuilder withVertexFormat(VertexFormat format, VertexFormat.Mode mode) {
        builder.withVertexFormat(format, mode);
        return this;
    }

    public ExtendedRenderPipelineBuilder withVertexShader(Identifier shader) {
        builder.withVertexShader(shader);
        return this;
    }

    public ExtendedRenderPipelineBuilder withFragmentShader(Identifier shader) {
        builder.withFragmentShader(shader);
        return this;
    }

    public ExtendedRenderPipelineBuilder withSampler(String sampler) {
        builder.withSampler(sampler);
        return this;
    }

    public ExtendedRenderPipelineBuilder withUniform(String uniform, UniformType type) {
        builder.withUniform(uniform, type);
        return this;
    }

    public ExtendedRenderPipelineBuilder withCull(boolean cull) {
        builder.withCull(cull);
        return this;
    }

    public ExtendedRenderPipelineBuilder withDepthTestFunction(CompareOp depthTest) {
        this.depthTest = depthTest;
        return this;
    }

    public ExtendedRenderPipelineBuilder withDepthWrite(boolean writeDepth) {
        this.writeDepth = writeDepth;
        return this;
    }

    public ExtendedRenderPipelineBuilder withBlend(BlendFunction blendFunction) {
        this.blendFunction = blendFunction;
        return this;
    }

    public ExtendedRenderPipelineBuilder withLineSmooth() {
        lineSmooth = true;
        return this;
    }

    public RenderPipeline build() {
        if (depthTest != null || writeDepth != null) {
            builder.withDepthStencilState(new DepthStencilState(
                depthTest != null ? depthTest : DepthStencilState.DEFAULT.depthTest(),
                writeDepth != null ? writeDepth : DepthStencilState.DEFAULT.writeDepth()
            ));
        }

        if (blendFunction != null) {
            builder.withColorTargetState(new ColorTargetState(blendFunction));
        }

        RenderPipeline pipeline = builder.build();
        ((IRenderPipeline) pipeline).meteor$setLineSmooth(lineSmooth);

        return pipeline;
    }
}
