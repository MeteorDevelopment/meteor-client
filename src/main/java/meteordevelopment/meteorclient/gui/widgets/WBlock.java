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
import meteordevelopment.meteorclient.gui.renderer.Scissor;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.SimpleBlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class WBlock extends WWidget {
    private static final int TEXTURE_SIZE = 64;

    private static VertexConsumerProvider.Immediate IMMEDIATE;
    private static Texture DEPTH;
    private static ProjectionMatrix2 PROJECTION_TEXTURE;
    private static ProjectionMatrix2 PROJECTION_SCREEN;

    private static final Reference2ObjectMap<BlockState, Texture> TEXTURES = new Reference2ObjectOpenHashMap<>();

    protected BlockState state;
    protected boolean initialized;
    protected boolean chached;

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
        if (!initialized) {
            chached = !SimpleBlockRenderer.hasAnimatedTextures(state);
        }

        if (IMMEDIATE == null) {
            IMMEDIATE = VertexConsumerProvider.immediate(new BufferAllocator(1536));
            DEPTH = new Texture(TEXTURE_SIZE, TEXTURE_SIZE, TextureFormat.DEPTH32, FilterMode.NEAREST, FilterMode.NEAREST);
            PROJECTION_TEXTURE = new ProjectionMatrix2("Block widget texture projection", -100, 100, true);
            PROJECTION_SCREEN = new ProjectionMatrix2("Block widget screen projection", -100, 100, false);
        }

        if (chached) {
            Texture texture = TEXTURES.computeIfAbsent(state, WBlock::renderToTexture);
            renderer.texture(x, y, width, height, 0, texture);
        } else {
            Optional<Scissor> scissorOpt = renderer.getScissor();
            renderer.post(() -> renderDirectly(scissorOpt, state, (float) x, (float) y, (float) width, (float) height, (float) theme.scale(0.5d)));
        }
    }

    private static Texture renderToTexture(BlockState state) {
        Texture color = new Texture(TEXTURE_SIZE, TEXTURE_SIZE, TextureFormat.RGBA8, FilterMode.NEAREST, FilterMode.NEAREST);

        var commands = RenderSystem.getDevice().createCommandEncoder();
        commands.clearDepthTexture(DEPTH.getGlTexture(), 1);
        commands.clearColorTexture(color.getGlTexture(), 0);

        RenderSystem.outputColorTextureOverride = color.getGlTextureView();
        RenderSystem.outputDepthTextureOverride = DEPTH.getGlTextureView();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(PROJECTION_TEXTURE.set(TEXTURE_SIZE, TEXTURE_SIZE), ProjectionType.PERSPECTIVE);

        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        view.scale(TEXTURE_SIZE);

        view.rotateXYZ(30 * (float) (Math.PI / 180.0), 45 * (float) (Math.PI / 180.0), 0);
        view.scale(0.625f, 0.625f, -0.625f);
        view.translate(0.55f, 0, -0.5f);

        SimpleBlockRenderer.renderFull(null, BlockPos.ORIGIN, state, null, new MatrixStack(), MinecraftClient.getInstance().getRenderTickCounter().getDynamicDeltaTicks(), IMMEDIATE);
        IMMEDIATE.draw();

        view.popMatrix();

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.outputDepthTextureOverride = null;
        RenderSystem.outputColorTextureOverride = null;

        return color;
    }

    private static void renderDirectly(Optional<Scissor> scissorOpt, BlockState state, float x, float y, float width, float height, float scale) {
        Window window = MinecraftClient.getInstance().getWindow();

        int framebufferHeight = window.getFramebufferHeight();
        float canonicalY = framebufferHeight - y - height;

        if (scissorOpt.isPresent()) {
            Scissor scissor = scissorOpt.get();
            int canonicalScissorY = framebufferHeight - scissor.y - scissor.height;

            int x1 = Math.max((int) x, scissor.x);
            int y1 = Math.max((int) canonicalY, canonicalScissorY);
            int x2 = Math.min((int) (x + width), scissor.x + scissor.width);
            int y2 = Math.min((int) (canonicalY + height), canonicalScissorY + scissor.height);
            int w = x2 - x1;
            int h = y2 - y1;

            RenderSystem.enableScissorForRenderTypeDraws(x1, y1, w, h);
        } else {
            RenderSystem.enableScissorForRenderTypeDraws((int) x, (int) (canonicalY), (int) width, (int) height);
        }

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(PROJECTION_SCREEN.set(window.getFramebufferWidth(), window.getFramebufferHeight()), ProjectionType.PERSPECTIVE);

        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        view.translate(x, canonicalY, 0);
        view.scale(TEXTURE_SIZE * scale);

        view.rotateXYZ(30 * (float) (Math.PI / 180.0), 45 * (float) (Math.PI / 180.0), 0);
        view.scale(0.625f, 0.625f, -0.625f);
        view.translate(0.55f, 0, -0.5f);

        SimpleBlockRenderer.renderFull(null, BlockPos.ORIGIN, state, null, new MatrixStack(), MinecraftClient.getInstance().getRenderTickCounter().getDynamicDeltaTicks(), IMMEDIATE);
        IMMEDIATE.draw();

        view.popMatrix();

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.disableScissorForRenderTypeDraws();
    }

    public void setState(BlockState state) {
        this.state = state;
        this.initialized = false;
    }
}
