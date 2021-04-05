/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.render;

import minegame159.meteorclient.mixininterface.IMatrix4f;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Vec3;
import minegame159.meteorclient.utils.misc.Vec4;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class NametagUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Vec4 vec4 = new Vec4();
    private static final Vec4 mmMat4 = new Vec4();
    private static final Vec4 pmMat4 = new Vec4();
    private static final Vec3 camera = new Vec3();
    private static final Vec3 cameraNegated = new Vec3();
    private static Matrix4f model;
    private static Matrix4f projection;
    private static double windowScale;

    private static double scale;

    public static void onRender(MatrixStack matrices, Matrix4f projection) {
        model = matrices.peek().getModel().copy();
        NametagUtils.projection = projection;

        camera.set(mc.gameRenderer.getCamera().getPos());
        cameraNegated.set(camera);
        cameraNegated.negate();

        windowScale = mc.getWindow().calculateScaleFactor(1, mc.forcesUnicodeFont());
    }

    public static boolean to2D(Vec3 pos, double scale) {
        NametagUtils.scale = getScale(pos) * scale;

        vec4.set(cameraNegated.x + pos.x, cameraNegated.y + pos.y, cameraNegated.z + pos.z, 1);

        ((IMatrix4f) (Object) model).multiplyMatrix(vec4, mmMat4);
        ((IMatrix4f) (Object) projection).multiplyMatrix(mmMat4, pmMat4);

        if (pmMat4.w <= 0.0f) return false;

        pmMat4.toScreen();
        double x = pmMat4.x * mc.getWindow().getFramebufferWidth();
        double y = pmMat4.y * mc.getWindow().getFramebufferHeight();

        if (Double.isInfinite(x) || Double.isInfinite(y)) return false;

        pos.set(x / windowScale, mc.getWindow().getFramebufferHeight() - y / windowScale, pmMat4.z);
        return true;
    }

    public static void begin(Vec3 pos) {
        glPushMatrix();
        glTranslated(pos.x, pos.y, 0);
        glScaled(scale, scale, 1);
    }

    public static void end() {
        glPopMatrix();
    }

    private static double getScale(Vec3 pos) {
        double dist = camera.distanceTo(pos);
        return Utils.clamp(1 - dist * 0.01, 0.5, Integer.MAX_VALUE);
    }
}
