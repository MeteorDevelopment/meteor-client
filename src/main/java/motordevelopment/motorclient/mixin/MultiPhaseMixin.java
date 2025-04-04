/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.IMultiPhase;
import net.minecraft.client.render.RenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhase.class)
public abstract class MultiPhaseMixin implements IMultiPhase {
    @Shadow
    @Final
    private RenderLayer.MultiPhaseParameters phases;

    @Override
    public RenderLayer.MultiPhaseParameters motor$getParameters() {
        return this.phases;
    }
}
