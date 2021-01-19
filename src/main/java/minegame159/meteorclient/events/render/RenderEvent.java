/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.events.render;

public class RenderEvent {
    private static final RenderEvent INSTANCE = new RenderEvent();

    public float tickDelta;
    public double offsetX, offsetY, offsetZ;


    public static RenderEvent get(float tickDelta, double offsetX, double offsetY, double offsetZ) {
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.offsetX = offsetX;
        INSTANCE.offsetY = offsetY;
        INSTANCE.offsetZ = offsetZ;
        return INSTANCE;
    }
}
