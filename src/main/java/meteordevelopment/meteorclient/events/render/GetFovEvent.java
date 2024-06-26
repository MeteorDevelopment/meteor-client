/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

public class GetFovEvent {
    private static final GetFovEvent INSTANCE = new GetFovEvent();

    public double fov;

    public static GetFovEvent get(double fov) {
        INSTANCE.fov = fov;
        return INSTANCE;
    }
}
