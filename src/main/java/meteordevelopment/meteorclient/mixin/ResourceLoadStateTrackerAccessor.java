/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.ResourceLoadStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceLoadStateTracker.class)
public interface ResourceLoadStateTrackerAccessor {
    @Accessor("reloadState")
    ResourceLoadStateTracker.ReloadState meteor$getReloadState();
}
