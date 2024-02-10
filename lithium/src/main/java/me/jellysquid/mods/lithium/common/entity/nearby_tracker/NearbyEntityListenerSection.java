package me.jellysquid.mods.lithium.common.entity.nearby_tracker;

import net.minecraft.world.entity.SectionedEntityCache;

public interface NearbyEntityListenerSection {
    void addListener(NearbyEntityListener listener);

    void removeListener(SectionedEntityCache<?> sectionedEntityCache, NearbyEntityListener listener);
}
