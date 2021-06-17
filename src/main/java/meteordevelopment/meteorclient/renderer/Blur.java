/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;

import static meteordevelopment.meteorclient.utils.Utils.mc;

public class Blur {
    private static Shader shader;
    private static Framebuffer fbo;

    public static void init() {
        shader = new Shader("blur.vert", "blur.frag");
        fbo = new Framebuffer();
    }

    public static void render() {
        if (GuiThemes.get().blur() == 0 || !(mc.currentScreen instanceof WidgetScreen)) return;

        int sourceTexture = mc.getFramebuffer().getColorAttachment();
        boolean horizontal = true;

        shader.bind();
        shader.set("u_Size", mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        shader.set("u_Texture", 0);

        PostProcessRenderer.beginRender();

        for (int i = 0; i < GuiThemes.get().blur() * 2; i++) {
            if (horizontal) {
                fbo.bind();
                GL.bindTexture(sourceTexture);
            }
            else {
                fbo.unbind();
                GL.bindTexture(fbo.texture);
            }

            shader.set("u_Horizontal", horizontal);
            PostProcessRenderer.render();

            horizontal = !horizontal;
        }

        PostProcessRenderer.endRender();
    }

    public static void onResized() {
        if (fbo != null) fbo.resize();
    }
}
