/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.entity.player;

import net.minecraft.fluid.FluidState;

public class CanWalkOnFluidEvent {
    private static final CanWalkOnFluidEvent INSTANCE = new CanWalkOnFluidEvent();

    public FluidState fluidState;
    public boolean walkOnFluid;

    public static CanWalkOnFluidEvent get(FluidState fluid) {
        INSTANCE.fluidState = fluid;
        INSTANCE.walkOnFluid = false;
        return INSTANCE;
    }
}
