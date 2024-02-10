package me.jellysquid.mods.lithium.common.entity.movement_tracker;

import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;

public interface EntityMovementTrackerSection {
    void addListener(SectionedEntityMovementTracker<?, ?> listener);

    void removeListener(SectionedEntityCache<?> sectionedEntityCache, SectionedEntityMovementTracker<?, ?> listener);

    void trackEntityMovement(int notificationMask, long time);

    long getChangeTime(int trackedClass);

    <S, E extends EntityLike> void listenToMovementOnce(SectionedEntityMovementTracker<E, S> listener, int trackedClass);

    <S, E extends EntityLike> void removeListenToMovementOnce(SectionedEntityMovementTracker<E, S> listener, int trackedClass);
}
