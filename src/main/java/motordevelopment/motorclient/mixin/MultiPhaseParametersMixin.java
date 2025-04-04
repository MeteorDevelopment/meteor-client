/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.IMultiPhaseParameters;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhaseParameters.class)
public abstract class MultiPhaseParametersMixin implements IMultiPhaseParameters {
    @Shadow
    @Final
    private RenderPhase.Target target;

    @Override
    public RenderPhase.Target motor$getTarget() {
        return this.target;
    }
}
