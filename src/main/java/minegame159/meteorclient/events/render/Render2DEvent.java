/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.render;

public class Render2DEvent {
    private static final Render2DEvent INSTANCE = new Render2DEvent();

    public int screenWidth, screenHeight;
    public float tickDelta;

    public static Render2DEvent get(int screenWidth, int screenHeight, float tickDelta) {
        INSTANCE.screenWidth = screenWidth;
        INSTANCE.screenHeight = screenHeight;
        INSTANCE.tickDelta = tickDelta;
        return INSTANCE;
    }
}
