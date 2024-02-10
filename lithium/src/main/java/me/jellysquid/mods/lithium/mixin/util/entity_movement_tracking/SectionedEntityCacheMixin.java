package me.jellysquid.mods.lithium.mixin.util.entity_movement_tracking;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.lithium.common.entity.movement_tracker.MovementTrackerCache;
import me.jellysquid.mods.lithium.common.entity.movement_tracker.SectionedEntityMovementTracker;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SectionedEntityCache.class)
public class SectionedEntityCacheMixin<T extends EntityLike> implements MovementTrackerCache {

    private final Object2ReferenceOpenHashMap<SectionedEntityMovementTracker<?, ?>, SectionedEntityMovementTracker<?, ?>> sectionEntityMovementTrackers = new Object2ReferenceOpenHashMap<>();

    @Override
    public void remove(SectionedEntityMovementTracker<?, ?> tracker) {
        this.sectionEntityMovementTrackers.remove(tracker);
    }

    @Override
    public <S extends SectionedEntityMovementTracker<?, ?>> S deduplicate(S tracker) {
        //noinspection unchecked
        S storedTracker = (S) this.sectionEntityMovementTrackers.putIfAbsent(tracker, tracker);
        return storedTracker == null ? tracker : storedTracker;
    }
}
