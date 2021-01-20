/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.rendering;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;

public class Matrices {
    private static MatrixStack matrixStack;

    public static void begin(MatrixStack matrixStack) {
        Matrices.matrixStack = matrixStack;
    }

    public static void push() {
        matrixStack.push();
    }

    public static void translate(double x, double y, double z) {
        matrixStack.translate(x, y, z);
    }

    public static void rotate(double angle, double x, double y, double z) {
        matrixStack.multiply(new Quaternion((float) (x * angle), (float) (y * angle), (float) (z * angle), true));
    }

    public static void scale(double x, double y, double z) {
        matrixStack.scale((float) x, (float) y, (float) z);
    }

    public static void pop() {
        matrixStack.pop();
    }

    public static Matrix4f getTop() {
        return matrixStack.peek().getModel();
    }

    public static MatrixStack getMatrixStack() {
        return matrixStack;
    }
}
