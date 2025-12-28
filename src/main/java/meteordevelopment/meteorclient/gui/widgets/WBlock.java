/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.SimpleBlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class WBlock extends WWidget {
    private static final int TEXTURE_SIZE = 64;

    private static VertexConsumerProvider.Immediate IMMEDIATE;
    private static Texture DEPTH;
    private static ProjectionMatrix2 PROJECTION;

    private static final Reference2ObjectMap<BlockState, BlockRenderData> TEXTURES = new Reference2ObjectOpenHashMap<>();

    protected BlockState state;

    public WBlock(BlockState state) {
        this.state = state;
    }

    @Override
    protected void onCalculateSize() {
        double s = theme.scale(32);

        width = s;
        height = s;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (state.isAir()) return;

        // Render as an item model
        ItemStack stack = state.getBlock().asItem().getDefaultStack();

        if (!stack.isEmpty()) {
            renderer.post(() -> {
                double s = theme.scale(2);
                renderer.item(stack, (int) x, (int) y, (float) s, true);
            });

            return;
        }

        // Render block
        Texture texture;

        @Nullable BlockRenderData renderData = TEXTURES.get(state);
        if (renderData != null) {
            texture = renderData.texture();
            if (renderData.animated()) {
                renderBlock(texture, state);
            }
        } else {
            texture = renderBlock(null, state);
            TEXTURES.put(state, new BlockRenderData(
                texture,
                SimpleBlockRenderer.hasAnimatedTextures(state)
            ));
        }

        renderer.texture(x, y, width, height, 0, texture);
    }

    private static Texture renderBlock(@Nullable Texture color, BlockState state) {
        if (IMMEDIATE == null) {
            IMMEDIATE = VertexConsumerProvider.immediate(new BufferAllocator(1536));
            DEPTH = new Texture(TEXTURE_SIZE, TEXTURE_SIZE, TextureFormat.DEPTH32, FilterMode.NEAREST, FilterMode.NEAREST);
            PROJECTION = new ProjectionMatrix2("Offscreen block renderer", -100, 100, true);
        }

        if (color == null) {
            color = new Texture(TEXTURE_SIZE, TEXTURE_SIZE, TextureFormat.RGBA8, FilterMode.NEAREST, FilterMode.NEAREST);
        }

        var commands = RenderSystem.getDevice().createCommandEncoder();
        commands.clearDepthTexture(DEPTH.getGlTexture(), 1);
        commands.clearColorTexture(color.getGlTexture(), 0);

        RenderSystem.outputColorTextureOverride = color.getGlTextureView();
        RenderSystem.outputDepthTextureOverride = DEPTH.getGlTextureView();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(PROJECTION.set(TEXTURE_SIZE, TEXTURE_SIZE), ProjectionType.PERSPECTIVE);

        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        view.scale(TEXTURE_SIZE);

        view.rotateXYZ(30 * (float) (Math.PI / 180.0), 45 * (float) (Math.PI / 180.0), 0);
        view.scale(0.625f, 0.625f, -0.625f);
        view.translate(0.5f, 0, -0.5f);

        SimpleBlockRenderer.renderFull(null, BlockPos.ORIGIN, state, null, new MatrixStack(), MinecraftClient.getInstance().getRenderTickCounter().getDynamicDeltaTicks(), IMMEDIATE);
        IMMEDIATE.draw();

        view.popMatrix();

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.outputDepthTextureOverride = null;
        RenderSystem.outputColorTextureOverride = null;

        return color;
    }

    public void setState(BlockState state) {
        this.state = state;
    }

    private record BlockRenderData(Texture texture, boolean animated) {}
}
