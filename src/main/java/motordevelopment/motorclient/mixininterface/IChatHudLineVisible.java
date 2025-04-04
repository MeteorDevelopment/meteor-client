/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

public interface IChatHudLineVisible extends IChatHudLine {
    boolean motor$isStartOfEntry();
    void motor$setStartOfEntry(boolean start);
}
