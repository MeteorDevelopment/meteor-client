/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.render;

import minegame159.meteorclient.mixininterface.IMatrix4f;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Vec4;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import static org.lwjgl.opengl.GL11.*;

public class NametagUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Vec4 vec4 = new Vec4();
    private static final Vec4 mmMat4 = new Vec4();
    private static final Vec4 pmMat4 = new Vec4();

    private static Matrix4f model;
    private static Matrix4f projection;
    private static Vec3d camera, cameraNegated;
    private static double windowScale;

    private static double scale;

    public static void onRender(MatrixStack matrices, Matrix4f projection) {
        model = matrices.peek().getModel().copy();
        NametagUtils.projection = projection;

        camera = mc.gameRenderer.getCamera().getPos();
        cameraNegated = camera.negate();

        windowScale = mc.getWindow().calculateScaleFactor(1, mc.forcesUnicodeFont());
    }

    public static boolean to2D(Vec3d pos, double scale) {
        NametagUtils.scale = getScale(pos) * scale;

        vec4.set(cameraNegated.x + pos.getX(), cameraNegated.y + pos.getY(), cameraNegated.z + pos.getZ(), 1);

        ((IMatrix4f) (Object) model).multiplyMatrix(vec4, mmMat4);
        ((IMatrix4f) (Object) projection).multiplyMatrix(mmMat4, pmMat4);

        if (pmMat4.w <= 0.0f) return false;

        pmMat4.toScreen();
        double x = pmMat4.x * mc.getWindow().getFramebufferWidth();
        double y = pmMat4.y * mc.getWindow().getFramebufferHeight();

        if (Double.isInfinite(x) || Double.isInfinite(y)) return false;

        ((IVec3d) pos).set(x / windowScale, mc.getWindow().getFramebufferHeight() - y / windowScale, pmMat4.z);
        return true;
    }

    public static void begin(Vec3d pos) {
        glPushMatrix();
        glTranslated(pos.x, pos.y, 0);
        glScaled(scale, scale, 1);
    }

    public static void end() {
        glPopMatrix();
    }

    private static double getScale(Vec3d pos) {
        double dist = camera.distanceTo(pos);
        return Utils.clamp(1 - dist * 0.01, 0.5, Integer.MAX_VALUE);
    }
}
