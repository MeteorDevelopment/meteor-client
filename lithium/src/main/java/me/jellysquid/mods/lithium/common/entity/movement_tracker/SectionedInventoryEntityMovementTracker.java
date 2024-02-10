package me.jellysquid.mods.lithium.common.entity.movement_tracker;

import me.jellysquid.mods.lithium.common.util.tuples.WorldSectionBox;
import me.jellysquid.mods.lithium.mixin.block.hopper.EntityTrackingSectionAccessor;
import me.jellysquid.mods.lithium.mixin.util.entity_movement_tracking.ServerEntityManagerAccessor;
import me.jellysquid.mods.lithium.mixin.util.entity_movement_tracking.ServerWorldAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class SectionedInventoryEntityMovementTracker<S> extends SectionedEntityMovementTracker<Entity, S> {

    public SectionedInventoryEntityMovementTracker(WorldSectionBox entityAccessBox, Class<S> clazz) {
        super(entityAccessBox, clazz);
    }

    public static <S> SectionedInventoryEntityMovementTracker<S> registerAt(ServerWorld world, Box interactionArea, Class<S> clazz) {
        MovementTrackerCache cache = (MovementTrackerCache) ((ServerEntityManagerAccessor<?>) ((ServerWorldAccessor) world).getEntityManager()).getCache();

        WorldSectionBox worldSectionBox = WorldSectionBox.entityAccessBox(world, interactionArea);
        SectionedInventoryEntityMovementTracker<S> tracker = new SectionedInventoryEntityMovementTracker<>(worldSectionBox, clazz);
        tracker = cache.deduplicate(tracker);

        tracker.register(world);
        return tracker;
    }

    public List<S> getEntities(Box box) {
        ArrayList<S> entities = new ArrayList<>();
        for (int i = 0; i < this.sortedSections.size(); i++) {
            if (this.sectionVisible[i]) {
                //noinspection unchecked
                TypeFilterableList<S> collection = ((EntityTrackingSectionAccessor<S>) this.sortedSections.get(i)).getCollection();

                for (S entity : collection.getAllOfType(this.clazz)) {
                    Entity inventoryEntity = (Entity) entity;
                    if (inventoryEntity.isAlive() && inventoryEntity.getBoundingBox().intersects(box)) {
                        entities.add(entity);
                    }
                }
            }
        }
        return entities;
    }
}
