/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.mixin.WorldRendererAccessor;
import minegame159.meteorclient.renderer.PostProcessRenderer;
import minegame159.meteorclient.renderer.Shader;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.ESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import org.lwjgl.opengl.GL32C;

import static minegame159.meteorclient.utils.Utils.mc;

public class Outlines {
    public static boolean loadingOutlineShader;
    public static boolean renderingOutlines;

    public static Framebuffer outlinesFbo;
    public static OutlineVertexConsumerProvider vertexConsumerProvider;
    private static Shader outlinesShader;

    public static void load() {
        loadingOutlineShader = true;
        outlinesShader = new Shader("outline.vert", "outline.frag");
        outlinesFbo = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false, false);
        vertexConsumerProvider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
        loadingOutlineShader = false;
    }

    public static void beginRender() {
        outlinesFbo.clear(MinecraftClient.IS_SYSTEM_MAC);
        mc.getFramebuffer().beginWrite(false);
    }

    public static void endRender() {
        WorldRenderer worldRenderer = mc.worldRenderer;
        WorldRendererAccessor wra = (WorldRendererAccessor) worldRenderer;

        Framebuffer fbo = worldRenderer.getEntityOutlinesFramebuffer();
        wra.setEntityOutlinesFramebuffer(outlinesFbo);
        vertexConsumerProvider.draw();
        wra.setEntityOutlinesFramebuffer(fbo);

        ESP esp = Modules.get().get(ESP.class);

        mc.getFramebuffer().beginWrite(false);

        GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
        GlStateManager._bindTexture(outlinesFbo.getColorAttachment());

        outlinesShader.bind();
        outlinesShader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        outlinesShader.set("u_Texture", 0);
        outlinesShader.set("u_Width", (double) esp.outlineWidth.get());
        outlinesShader.set("u_FillOpacity", esp.fillOpacity.get().floatValue() / 255.0);
        outlinesShader.set("u_ShapeMode", (double) esp.shapeMode.get().ordinal());
        PostProcessRenderer.render();

        GlStateManager._bindTexture(0);
    }

    public static void onResized(int width, int height) {
        if (outlinesFbo != null) outlinesFbo.resize(width, height, false);
    }
}
