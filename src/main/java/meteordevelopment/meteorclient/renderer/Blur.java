/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL32C;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class Blur {
    private static final int ITERATIONS = 4;
    private static final int OFFSET = 4;

    private static Shader shaderDown, shaderUp;

    public static void init() {
        shaderDown = new Shader("simple.vert", "kawase_down.frag");
        shaderUp = new Shader("simple.vert", "kawase_up.frag");
    }

    public static void render() {
        if (!GuiThemes.get().blur() || !(mc.currentScreen instanceof WidgetScreen)) return;

        Framebuffer fbo = mc.getFramebuffer();

        // Bind framebuffer texture and begin post process quad rendering
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0);
        GlStateManager._bindTexture(fbo.getColorAttachment());

        // Apply kawase down
        shaderDown.bind();
        shaderDown.set("u_Texture", 0);
        shaderDown.set("u_Size", fbo.textureWidth, fbo.textureHeight);
        shaderDown.set("u_Offset", OFFSET, OFFSET);
        shaderDown.set("u_HalfPixel", 0.5 / fbo.textureWidth, 0.5 / fbo.textureHeight);

        for (int i = 0; i < ITERATIONS; i++) PostProcessRenderer.render();

        // Apply kawase up
        shaderUp.bind();
        shaderUp.set("u_Texture", 0);
        shaderUp.set("u_Size", fbo.textureWidth, fbo.textureHeight);
        shaderUp.set("u_Offset", OFFSET, OFFSET);
        shaderUp.set("u_HalfPixel", 0.5 / fbo.textureWidth, 0.5 / fbo.textureHeight);
        shaderUp.set("u_Alpha", 1f);

        for (int i = 0; i < ITERATIONS; i++) {
            if (i == ITERATIONS - 1) shaderUp.set("u_Alpha", (float) ((WidgetScreen) mc.currentScreen).animProgress);

            PostProcessRenderer.render();
        }

        // Unbind framebuffer texture and end post process quad rendering
        GlStateManager._bindTexture(0);
    }
}
