/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import meteordevelopment.meteorclient.mixininterface.ICapabilityTracker;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;

import static org.lwjgl.opengl.GL11C.*;

public class GL {
    private static final ICapabilityTracker DEPTH = getTracker("DEPTH");
    private static final ICapabilityTracker BLEND = getTracker("BLEND");
    private static final ICapabilityTracker CULL = getTracker("CULL");

    private static boolean depthSaved, blendSaved, cullSaved;

    public static void saveState() {
        depthSaved = DEPTH.get();
        blendSaved = BLEND.get();
        cullSaved = CULL.get();
    }

    public static void restoreState() {
        DEPTH.set(depthSaved);
        BLEND.set(blendSaved);
        CULL.set(cullSaved);

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
        GlStateManager._blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
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
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);
    }
    public static void disableLineSmooth() {
        glDisable(GL_LINE_SMOOTH);
    }

    private static ICapabilityTracker getTracker(String fieldName) {
        try {
            Class<?> glStateManager = GlStateManager.class;

            Field field = glStateManager.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object state = field.get(null);

            String trackerName = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "com.mojang.blaze3d.platform.GlStateManager$class_1018");

            Field capStateField = null;
            for (Field f : state.getClass().getDeclaredFields()) {
                if (f.getType().getName().equals(trackerName)) {
                    capStateField = f;
                    break;
                }
            }

            capStateField.setAccessible(true);
            return (ICapabilityTracker) capStateField.get(state);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
