/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixininterface;

import net.minecraft.client.render.RenderLayer;

public interface IMultiPhase {
    RenderLayer.MultiPhaseParameters motor$getParameters();
}
