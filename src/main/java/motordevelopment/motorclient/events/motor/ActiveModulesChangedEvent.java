/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.motor;

public class ActiveModulesChangedEvent {
    private static final ActiveModulesChangedEvent INSTANCE = new ActiveModulesChangedEvent();

    public static ActiveModulesChangedEvent get() {
        return INSTANCE;
    }
}
