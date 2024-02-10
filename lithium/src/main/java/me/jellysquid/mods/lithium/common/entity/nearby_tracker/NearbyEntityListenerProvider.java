package me.jellysquid.mods.lithium.common.entity.nearby_tracker;

import org.jetbrains.annotations.Nullable;

public interface NearbyEntityListenerProvider {
    @Nullable
    NearbyEntityListenerMulti getListener();

    void addListener(NearbyEntityTracker listener);
}
