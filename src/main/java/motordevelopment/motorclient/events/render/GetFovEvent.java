/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.render;

public class GetFovEvent {
    private static final GetFovEvent INSTANCE = new GetFovEvent();

    public float fov;

    public static GetFovEvent get(float fov) {
        INSTANCE.fov = fov;
        return INSTANCE;
    }
}
