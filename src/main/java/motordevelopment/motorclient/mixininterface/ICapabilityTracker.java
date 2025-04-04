/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

public interface ICapabilityTracker {
    boolean motor$get();

    void motor$set(boolean state);
}
