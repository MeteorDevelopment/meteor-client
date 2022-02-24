/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.PostProcessRenderer;
import meteordevelopment.meteorclient.renderer.Shader;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityShaders {
    // Overlay
    public static Framebuffer overlayFramebuffer;
    public static OutlineVertexConsumerProvider overlayVertexConsumerProvider;
    private static Shader overlayShader;

    private static Chams chams;
    public static float timer;

    // Outline
    public static Framebuffer outlinesFramebuffer;
    public static OutlineVertexConsumerProvider outlinesVertexConsumerProvider;
    private static Shader outlinesShader;

    private static ESP esp;
    public static boolean renderingOutlines;

    // Overlay

    public static void initOverlay(String shaderName) {
        overlayShader = new Shader("outline.vert", shaderName + ".frag");
        overlayFramebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false, false);
        overlayVertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
        timer = 0;
    }

    public static boolean shouldDrawOverlay(Entity entity) {
        if (chams == null) chams = Modules.get().get(Chams.class);
        return chams.isShader() && chams.entities.get().getBoolean(entity.getType()) && (entity != mc.player || !chams.ignoreSelfDepth.get());
    }

    // Outlines

    @Init(stage = InitStage.Pre)
    public static void initOutlines() {
        outlinesShader = new Shader("outline.vert", "outline.frag");
        outlinesFramebuffer = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false, false);
        outlinesVertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
    }

    public static boolean shouldDrawOutline(Entity entity) {
        if (esp == null) esp = Modules.get().get(ESP.class);
        return esp.isShader() && esp.getOutlineColor(entity) != null && (entity != mc.player || !esp.ignoreSelf.get());
    }

    // Main

    public static void beginRender() {
        // Overlay
        if (chams == null) chams = Modules.get().get(Chams.class);
        if (chams.isShader()) overlayFramebuffer.clear(false);

        // Outline
        if (esp == null) esp = Modules.get().get(ESP.class);
        if (esp.isShader()) outlinesFramebuffer.clear(false);

        mc.getFramebuffer().beginWrite(false);
    }

    public static void endRender() {
        WorldRenderer worldRenderer = mc.worldRenderer;
        WorldRendererAccessor wra = (WorldRendererAccessor) worldRenderer;
        Framebuffer fbo = worldRenderer.getEntityOutlinesFramebuffer();

        // Overlay
        if (chams != null && chams.isShader()) {
            wra.setEntityOutlinesFramebuffer(overlayFramebuffer);
            overlayVertexConsumerProvider.draw();
            wra.setEntityOutlinesFramebuffer(fbo);

            mc.getFramebuffer().beginWrite(false);

            GL.bindTexture(overlayFramebuffer.getColorAttachment());

            overlayShader.bind();
            overlayShader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
            overlayShader.set("u_Texture", 0);
            overlayShader.set("u_Time", timer++ / 20.0);
            PostProcessRenderer.render();
        }

        // Outline
        if (esp != null && esp.isShader()) renderOutlines(outlinesVertexConsumerProvider::draw, true, esp.outlineWidth.get(), esp.fillOpacity.get().floatValue(), esp.shapeMode.get());
    }

    public static void onResized(int width, int height) {
        if (overlayFramebuffer != null) overlayFramebuffer.resize(width, height, false);
        if (outlinesFramebuffer != null) outlinesFramebuffer.resize(width, height, false);
    }

    public static void renderOutlines(Runnable draw, boolean entities, int width, float fillOpacity, ShapeMode shapeMode) {
        WorldRenderer worldRenderer = mc.worldRenderer;
        WorldRendererAccessor wra = (WorldRendererAccessor) worldRenderer;
        Framebuffer fbo = worldRenderer.getEntityOutlinesFramebuffer();

        if (entities) wra.setEntityOutlinesFramebuffer(outlinesFramebuffer);
        else {
            outlinesFramebuffer.clear(false);
            outlinesFramebuffer.beginWrite(false);
        }
        draw.run();
        if (entities) wra.setEntityOutlinesFramebuffer(fbo);

        mc.getFramebuffer().beginWrite(false);

        GL.bindTexture(outlinesFramebuffer.getColorAttachment());

        outlinesShader.bind();
        outlinesShader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        outlinesShader.set("u_Texture", 0);
        outlinesShader.set("u_Width", width);
        outlinesShader.set("u_FillOpacity", fillOpacity / 255.0);
        outlinesShader.set("u_ShapeMode", shapeMode.ordinal());
        PostProcessRenderer.render();
    }
}
