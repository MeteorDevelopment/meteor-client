/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.mixininterface.ICapabilityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$CapabilityTracker")
public abstract class CapabilityTrackerMixin implements ICapabilityTracker {
    @Shadow
    private boolean state;

    @Shadow
    public abstract void setState(boolean state);

    @Override
    public boolean motor$get() {
        return state;
    }

    @Override
    public void motor$set(boolean state) {
        setState(state);
    }
}
