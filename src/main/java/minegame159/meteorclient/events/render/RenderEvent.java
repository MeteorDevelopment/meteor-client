/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.render;

import net.minecraft.client.util.math.MatrixStack;

public class RenderEvent {
    private static final RenderEvent INSTANCE = new RenderEvent();

    public MatrixStack matrices;
    public float tickDelta;
    public double offsetX, offsetY, offsetZ;

    public static RenderEvent get(MatrixStack matrices, float tickDelta, double offsetX, double offsetY, double offsetZ) {
        INSTANCE.matrices = matrices;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.offsetX = offsetX;
        INSTANCE.offsetY = offsetY;
        INSTANCE.offsetZ = offsetZ;
        return INSTANCE;
    }
}
