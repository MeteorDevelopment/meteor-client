/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import static org.lwjgl.opengl.GL11.*;

public class PostProcessRenderer {
    private static Mesh mesh;

    public static void init() {
        float[] vertices = {
                -1, -1,
                -1, 1,
                1, 1,
                1, -1
        };

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        mesh = new Mesh(vertices, indices, 2);
    }

    public static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.disableLighting();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();

        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();

        mesh.bind();
    }

    public static void renderMesh() {
        mesh.renderMesh();
    }

    public static void end() {
        mesh.unbind();

        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();

        RenderSystem.enableDepthTest();
    }

    public static void render() {
        begin();
        renderMesh();
        end();
    }
}
