/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.game;

public class ResolutionChangedEvent {
    private static final ResolutionChangedEvent INSTANCE = new ResolutionChangedEvent();

    public static ResolutionChangedEvent get() {
        return INSTANCE;
    }
}
