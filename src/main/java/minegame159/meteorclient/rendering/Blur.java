/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.gui.GuiThemes;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.rendering.gl.PostProcessRenderer;
import minegame159.meteorclient.rendering.gl.Shader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

public class Blur {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final int ITERATIONS = 4;
    private static final int OFFSET = 4;

    private static Shader shaderDown, shaderUp;

    public static void init() {
        shaderDown = new Shader("shaders/simple.vert", "shaders/kawase_down.frag");
        shaderUp = new Shader("shaders/simple.vert", "shaders/kawase_up.frag");
    }

    public static void render() {
        if (!GuiThemes.get().blur() || !(mc.currentScreen instanceof WidgetScreen)) return;

        Framebuffer fbo = mc.getFramebuffer();

        // Bind framebuffer texture and begin post process quad rendering
        GlStateManager.activeTexture(GL_TEXTURE0);
        GlStateManager.bindTexture(fbo.getColorAttachment());

        PostProcessRenderer.begin();

        // Apply kawase down
        shaderDown.bind();
        shaderDown.set("u_Texture", 0);
        shaderDown.set("u_Size", fbo.textureWidth, fbo.textureHeight);
        shaderDown.set("u_Offset", (float) OFFSET, (float) OFFSET);
        shaderDown.set("u_HalfPixel", 0.5f / fbo.textureWidth, 0.5f / fbo.textureHeight);

        for (int i = 0; i < ITERATIONS; i++) PostProcessRenderer.renderMesh();

        // Apply kawase up
        shaderUp.bind();
        shaderUp.set("u_Texture", 0);
        shaderUp.set("u_Size", fbo.textureWidth, fbo.textureHeight);
        shaderUp.set("u_Offset", (float) OFFSET, (float) OFFSET);
        shaderUp.set("u_HalfPixel", 0.5f / fbo.textureWidth, 0.5f / fbo.textureHeight);
        shaderUp.set("u_Alpha", 1f);

        for (int i = 0; i < ITERATIONS; i++) {
            if (i == ITERATIONS - 1) shaderUp.set("u_Alpha", (float) ((WidgetScreen) mc.currentScreen).animProgress);

            PostProcessRenderer.renderMesh();
        }

        shaderUp.unbind();

        // Unbind framebuffer texture and end post process quad rendering
        GlStateManager.bindTexture(0);

        PostProcessRenderer.end();
    }
}
