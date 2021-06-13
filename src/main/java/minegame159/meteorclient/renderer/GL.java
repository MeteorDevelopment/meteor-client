/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11C;

public class GL {
    private static boolean depthSaved, blendSaved, cullSaved;

    public static void saveState() {
        depthSaved = GlStateManager.DEPTH.capState.state;
        blendSaved = GlStateManager.BLEND.capState.state;
        cullSaved = GlStateManager.CULL.capState.state;
    }

    public static void restoreState() {
        GlStateManager.DEPTH.capState.setState(depthSaved);
        GlStateManager.BLEND.capState.setState(blendSaved);
        GlStateManager.CULL.capState.setState(cullSaved);

        disableLineSmooth();
    }

    public static void enableDepth() {
        GlStateManager._enableDepthTest();
    }
    public static void disableDepth() {
        GlStateManager._disableDepthTest();
    }

    public static void enableBlend() {
        GlStateManager._enableBlend();
        GlStateManager._blendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA);
    }
    public static void disableBlend() {
        GlStateManager._disableBlend();
    }

    public static void enableCull() {
        GlStateManager._enableCull();
    }
    public static void disableCull() {
        GlStateManager._disableCull();
    }

    public static void enableLineSmooth() {
        GL11C.glEnable(GL11C.GL_LINE_SMOOTH);
        GL11C.glLineWidth(1);

    }
    public static void disableLineSmooth() {
        GL11C.glDisable(GL11C.GL_LINE_SMOOTH);
    }
}
