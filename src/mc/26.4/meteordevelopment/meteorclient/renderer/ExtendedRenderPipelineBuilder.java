/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.mixininterface.IRenderPipeline;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * In 26.3 {@link RenderPipeline.Builder} has a private constructor (only reachable via the
 * {@code RenderPipeline.builder(...)} factory), so it can no longer be extended. This class wraps a
 * builder instead and forwards every method Meteor uses, while still being able to set the extra
 * "line smooth" flag on the resulting pipeline.
 */
public class ExtendedRenderPipelineBuilder {
    private final RenderPipeline.Builder delegate;
    private boolean lineSmooth;

    public ExtendedRenderPipelineBuilder(RenderPipeline.Snippet... snippets) {
        delegate = RenderPipeline.builder(snippets);
    }

    public ExtendedRenderPipelineBuilder withLineSmooth() {
        lineSmooth = true;
        return this;
    }

    public ExtendedRenderPipelineBuilder withLocation(String location) {
        delegate.withLocation(location);
        return this;
    }

    public ExtendedRenderPipelineBuilder withLocation(Identifier location) {
        delegate.withLocation(location);
        return this;
    }

    public ExtendedRenderPipelineBuilder withFragmentShader(Identifier id) {
        delegate.withFragmentShader(id);
        return this;
    }

    public ExtendedRenderPipelineBuilder withVertexShader(Identifier id) {
        delegate.withVertexShader(id);
        return this;
    }

    public ExtendedRenderPipelineBuilder withBindGroupLayout(BindGroupLayout layout) {
        delegate.withBindGroupLayout(layout);
        return this;
    }

    public ExtendedRenderPipelineBuilder withDepthStencilState(DepthStencilState state) {
        delegate.withDepthStencilState(state);
        return this;
    }

    public ExtendedRenderPipelineBuilder withCull(boolean cull) {
        delegate.withCull(cull);
        return this;
    }

    public ExtendedRenderPipelineBuilder withColorTargetState(ColorTargetState state) {
        delegate.withColorTargetState(state);
        return this;
    }

    public ExtendedRenderPipelineBuilder withVertexBinding(int index, VertexFormat format) {
        delegate.withVertexBinding(index, format);
        return this;
    }

    public ExtendedRenderPipelineBuilder withPrimitiveTopology(PrimitiveTopology topology) {
        delegate.withPrimitiveTopology(topology);
        return this;
    }

    @NonNull
    public RenderPipeline build() {
        RenderPipeline pipeline = delegate.build();
        ((IRenderPipeline) pipeline).meteor$setLineSmooth(lineSmooth);

        return pipeline;
    }
}
