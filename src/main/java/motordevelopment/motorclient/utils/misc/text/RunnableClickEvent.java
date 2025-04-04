/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.utils.misc.text;

/**
 * Allows arbitrary code execution in a click event
 */
public class RunnableClickEvent extends MotorClickEvent {
    public final Runnable runnable;

    public RunnableClickEvent(Runnable runnable) {
        super(null, null); // Should ensure no vanilla code is triggered, and only we handle it
        this.runnable = runnable;
    }
}
