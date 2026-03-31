/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import meteordevelopment.meteorclient.mixininterface.ICapabilityTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlStateManager.BooleanState.class)
public abstract class CapabilityTrackerMixin implements ICapabilityTracker {
    @Shadow
    private boolean enabled;

    @Shadow
    public abstract void setEnabled(boolean state);

    @Override
    public boolean meteor$get() {
        return enabled;
    }

    @Override
    public void meteor$set(boolean state) {
        setEnabled(state);
    }
}
