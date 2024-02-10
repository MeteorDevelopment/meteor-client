package me.jellysquid.mods.lithium.common.world;

import net.minecraft.world.entity.EntityLike;

public interface ChunkAwareEntityIterable<T extends EntityLike> {
    Iterable<T> lithiumIterateEntitiesInTrackedSections();
}
