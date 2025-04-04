/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.render;

import motordevelopment.motorclient.renderer.Renderer3D;
import motordevelopment.motorclient.utils.Utils;
import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent {
    private static final Render3DEvent INSTANCE = new Render3DEvent();

    public MatrixStack matrices;
    public Renderer3D renderer;
    public double frameTime;
    public float tickDelta;
    public double offsetX, offsetY, offsetZ;

    public static Render3DEvent get(MatrixStack matrices, Renderer3D renderer, float tickDelta, double offsetX, double offsetY, double offsetZ) {
        INSTANCE.matrices = matrices;
        INSTANCE.renderer = renderer;
        INSTANCE.frameTime = Utils.frameTime;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.offsetX = offsetX;
        INSTANCE.offsetY = offsetY;
        INSTANCE.offsetZ = offsetZ;
        return INSTANCE;
    }
}
