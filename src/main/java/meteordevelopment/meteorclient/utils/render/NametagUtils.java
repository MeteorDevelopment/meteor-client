/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class NametagUtils {
    private static final Vector4f vec4 = new Vector4f();
    private static final Vector4f mmMat4 = new Vector4f();
    private static final Vector4f pmMat4 = new Vector4f();
    private static final Vector3d camera = new Vector3d();
    private static final Vector3d cameraNegated = new Vector3d();
    private static Matrix4f model;
    private static Matrix4f projection;
    private static double windowScale;

    public static double scale;

    public static void onRender(MatrixStack matrices, Matrix4f projection) {
        model = new Matrix4f(matrices.peek().getPositionMatrix());
        NametagUtils.projection = projection;

        Utils.set(camera, mc.gameRenderer.getCamera().getPos());
        cameraNegated.set(camera);
        cameraNegated.negate();

        windowScale = mc.getWindow().calculateScaleFactor(1, false);
    }

    public static boolean to2D(Vector3d pos, double scale) {
        return to2D(pos, scale, true);
    }

    public static boolean to2D(Vector3d pos, double scale, boolean distanceScaling) {
        return to2D(pos, scale, distanceScaling, false);
    }

    public static boolean to2D(Vector3d pos, double scale, boolean distanceScaling, boolean allowBehind) {
        NametagUtils.scale = scale;
        if (distanceScaling) {
            NametagUtils.scale *= getScale(pos);
        }

        vec4.set(cameraNegated.x + pos.x, cameraNegated.y + pos.y, cameraNegated.z + pos.z, 1);

        vec4.mul(model, mmMat4);
        mmMat4.mul(projection, pmMat4);

        boolean behind = pmMat4.w <= 0.f;

        if (behind && !allowBehind) return false;

        toScreen(pmMat4);
        double x = pmMat4.x * mc.getWindow().getFramebufferWidth();
        double y = pmMat4.y * mc.getWindow().getFramebufferHeight();

        if (behind) {
            x = mc.getWindow().getFramebufferWidth() - x;
            y = mc.getWindow().getFramebufferHeight() - y;
        }

        if (Double.isInfinite(x) || Double.isInfinite(y)) return false;

        pos.set(x / windowScale, mc.getWindow().getFramebufferHeight() - y / windowScale, allowBehind ? pmMat4.w : pmMat4.z);
        return true;
    }

    public static void begin(Vector3d pos) {
        MatrixStack matrices = RenderSystem.getModelViewStack();
        begin(matrices, pos);
    }

    public static void begin(Vector3d pos, DrawContext drawContext) {
        begin(pos);
        begin(drawContext.getMatrices(), pos);
    }

    private static void begin(MatrixStack matrices, Vector3d pos) {
        matrices.push();
        matrices.translate(pos.x, pos.y, 0);
        matrices.scale((float) scale, (float) scale, 1);
    }

    public static void end() {
        RenderSystem.getModelViewStack().pop();
    }

    public static void end(DrawContext drawContext) {
        end();
        drawContext.getMatrices().pop();
    }

    private static double getScale(Vector3d pos) {
        double dist = camera.distance(pos);
        return MathHelper.clamp(1 - dist * 0.01, 0.5, Integer.MAX_VALUE);
    }

    private static void toScreen(Vector4f vec) {
        float newW = 1.0f / vec.w * 0.5f;

        vec.x = vec.x * newW + 0.5f;
        vec.y = vec.y * newW + 0.5f;
        vec.z = vec.z * newW + 0.5f;
        vec.w = newW;
    }
}
