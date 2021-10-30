/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.PostProcessRenderer;
import meteordevelopment.meteorclient.renderer.Shader;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.utils.Init;
import meteordevelopment.meteorclient.utils.InitStage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Outlines {
    public static boolean renderingOutlines;

    public static Framebuffer outlinesFbo;
    public static OutlineVertexConsumerProvider vertexConsumerProvider;
    private static Shader outlinesShader;

    private static ESP esp;

    @Init(stage = InitStage.Pre)
    public static void init() {
        outlinesShader = new Shader("outline.vert", "outline.frag");
        outlinesFbo = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false, false);
        vertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
    }

    private static boolean shouldCancel() {
        return !esp.isActive() || !esp.isShader();
    }

    public static void beginRender() {
        if (esp == null) esp = Modules.get().get(ESP.class);
        if (shouldCancel()) return;

        outlinesFbo.clear(MinecraftClient.IS_SYSTEM_MAC);
        mc.getFramebuffer().beginWrite(false);
    }

    public static void endRender() {
        if (shouldCancel()) return;

        WorldRenderer worldRenderer = mc.worldRenderer;
        WorldRendererAccessor wra = (WorldRendererAccessor) worldRenderer;

        Framebuffer fbo = worldRenderer.getEntityOutlinesFramebuffer();
        wra.setEntityOutlinesFramebuffer(outlinesFbo);
        vertexConsumerProvider.draw();
        wra.setEntityOutlinesFramebuffer(fbo);

        mc.getFramebuffer().beginWrite(false);

        GL.bindTexture(outlinesFbo.getColorAttachment());

        outlinesShader.bind();
        outlinesShader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        outlinesShader.set("u_Texture", 0);
        outlinesShader.set("u_Width", esp.outlineWidth.get());
        outlinesShader.set("u_FillOpacity", esp.fillOpacity.get().floatValue() / 255.0);
        outlinesShader.set("u_ShapeMode", esp.shapeMode.get().ordinal());
        PostProcessRenderer.render();
    }

    public static void onResized(int width, int height) {
        if (outlinesFbo != null) outlinesFbo.resize(width, height, false);
    }
}
