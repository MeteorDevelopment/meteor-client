/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.render;

import minegame159.meteorclient.renderer.Renderer3D;
import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent {
    private static final Render3DEvent INSTANCE = new Render3DEvent();

    public MatrixStack matrices;
    public Renderer3D renderer;
    public float tickDelta;
    public double offsetX, offsetY, offsetZ;

    public static Render3DEvent get(MatrixStack matrices, Renderer3D renderer, float tickDelta, double offsetX, double offsetY, double offsetZ) {
        INSTANCE.matrices = matrices;
        INSTANCE.renderer = renderer;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.offsetX = offsetX;
        INSTANCE.offsetY = offsetY;
        INSTANCE.offsetZ = offsetZ;
        return INSTANCE;
    }
}
