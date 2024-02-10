package me.jellysquid.mods.lithium.common.entity.nearby_tracker;

import me.jellysquid.mods.lithium.common.util.tuples.Range6Int;
import net.minecraft.entity.Entity;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;

public interface NearbyEntityListener {
    Range6Int EMPTY_RANGE = new Range6Int(0, 0, 0, -1, -1, -1);
    /**
     * Calls the callbacks for the chunk coordinates that this listener is leaving and entering
     */
    default void updateChunkRegistrations(SectionedEntityCache<? extends EntityLike> entityCache, ChunkSectionPos prevCenterPos, Range6Int prevChunkRange, ChunkSectionPos newCenterPos, Range6Int newChunkRange) {
        if (newChunkRange == EMPTY_RANGE && prevChunkRange == EMPTY_RANGE) {
            return;
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();

        BlockBox after = newCenterPos == null ? null : new BlockBox(newCenterPos.getX() - newChunkRange.negativeX(), newCenterPos.getY() - newChunkRange.negativeY(), newCenterPos.getZ() - newChunkRange.negativeZ(), newCenterPos.getX() + newChunkRange.positiveX(), newCenterPos.getY() + newChunkRange.positiveY(), newCenterPos.getZ() + newChunkRange.positiveZ());
        BlockBox before = prevCenterPos == null ? null : new BlockBox(prevCenterPos.getX() - prevChunkRange.negativeX(), prevCenterPos.getY() - prevChunkRange.negativeY(), prevCenterPos.getZ() - prevChunkRange.negativeZ(), prevCenterPos.getX() + prevChunkRange.positiveX(), prevCenterPos.getY() + prevChunkRange.positiveY(), prevCenterPos.getZ() + prevChunkRange.positiveZ());
        if (before != null) {
            for (int x = before.getMinX(); x <= before.getMaxX(); x++) {
                for (int y = before.getMinY(); y <= before.getMaxY(); y++) {
                    for (int z = before.getMinZ(); z <= before.getMaxZ(); z++) {
                        if (after == null || !after.contains(pos.set(x, y, z))) {
                            long sectionPos = ChunkSectionPos.asLong(x, y, z);
                            EntityTrackingSection<? extends EntityLike> trackingSection = entityCache.getTrackingSection(sectionPos);
                            ((NearbyEntityListenerSection) trackingSection).removeListener(entityCache, this);
                            if (trackingSection.isEmpty()) {
                                entityCache.removeSection(sectionPos);
                            }
                        }
                    }
                }
            }
        }
        if (after != null) {
            for (int x = after.getMinX(); x <= after.getMaxX(); x++) {
                for (int y = after.getMinY(); y <= after.getMaxY(); y++) {
                    for (int z = after.getMinZ(); z <= after.getMaxZ(); z++) {
                        if (before == null || !before.contains(pos.set(x, y, z))) {
                            ((NearbyEntityListenerSection) entityCache.getTrackingSection(ChunkSectionPos.asLong(x, y, z))).addListener(this);
                        }
                    }
                }
            }
        }
    }

    default void removeFromAllChunksInRange(SectionedEntityCache<? extends EntityLike> entityCache, ChunkSectionPos prevCenterPos) {
        this.updateChunkRegistrations(entityCache, prevCenterPos, this.getChunkRange(), null, EMPTY_RANGE);
    }

    default void addToAllChunksInRange(SectionedEntityCache<? extends EntityLike> entityCache, ChunkSectionPos newCenterPos) {
        this.updateChunkRegistrations(entityCache, null, EMPTY_RANGE, newCenterPos, this.getChunkRange());
    }

    Range6Int getChunkRange();

    /**
     * Called by the entity tracker when an entity enters the range of this listener.
     */
    void onEntityEnteredRange(Entity entity);

    /**
     * Called by the entity tracker when an entity leaves the range of this listener or is removed from the world.
     */
    void onEntityLeftRange(Entity entity);

    default Class<? extends Entity> getEntityClass() {
        return Entity.class;
    }

    /**
     * Method to add all entities in the iteration order of the chunk section. This order is relevant and necessary
     * to keep vanilla parity.
     * @param <T> the type of the Entities in the collection
     * @param entityTrackingSection the section the entities are in
     * @param collection the collection of Entities that entered the range of this listener
     */
    default <T> void onSectionEnteredRange(Object entityTrackingSection, TypeFilterableList<T> collection) {
        for (Entity entity : collection.getAllOfType(this.getEntityClass())) {
            this.onEntityEnteredRange(entity);
        }
    }

    default <T> void onSectionLeftRange(Object entityTrackingSection, TypeFilterableList<T> collection) {
        for (Entity entity : collection.getAllOfType(this.getEntityClass())) {
            this.onEntityLeftRange(entity);
        }
    }

}
