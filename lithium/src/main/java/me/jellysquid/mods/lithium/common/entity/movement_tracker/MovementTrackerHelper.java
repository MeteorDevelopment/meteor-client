package me.jellysquid.mods.lithium.common.entity.movement_tracker;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import me.jellysquid.mods.lithium.api.inventory.LithiumInventory;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.entity.EntityLike;

import java.util.List;

/**
 * Helps to track in which entity sections entities of a certain type moved, appeared or disappeared by providing int
 * masks for all Entity classes.
 * Helps to track the entities within a world and provide notifications to listeners when a tracked entity enters or leaves a
 * watched area. This removes the necessity to constantly poll the world for nearby entities each tick and generally
 * provides a sizable boost to performance of hoppers.
 */
public abstract class MovementTrackerHelper {
    public static final List<Class<?>> MOVEMENT_NOTIFYING_ENTITY_CLASSES;
    public static volatile Reference2IntOpenHashMap<Class<? extends EntityLike>> CLASS_2_NOTIFY_MASK;
    public static final int NUM_MOVEMENT_NOTIFYING_CLASSES;

    static {
        if (LithiumInventory.class.isAssignableFrom(HopperBlockEntity.class)) {
            MOVEMENT_NOTIFYING_ENTITY_CLASSES = List.of(ItemEntity.class, Inventory.class);
        } else {
            MOVEMENT_NOTIFYING_ENTITY_CLASSES = List.of();
        }

        CLASS_2_NOTIFY_MASK = new Reference2IntOpenHashMap<>();
        CLASS_2_NOTIFY_MASK.defaultReturnValue(-1);
        NUM_MOVEMENT_NOTIFYING_CLASSES = MOVEMENT_NOTIFYING_ENTITY_CLASSES.size();
    }

    public static int getNotificationMask(Class<? extends EntityLike> entityClass) {
        int notificationMask = CLASS_2_NOTIFY_MASK.getInt(entityClass);
        if (notificationMask == -1) {
            notificationMask = calculateNotificationMask(entityClass);
        }
        return notificationMask;
    }
    private static int calculateNotificationMask(Class<? extends EntityLike> entityClass) {
        int mask = 0;
        for (int i = 0; i < MOVEMENT_NOTIFYING_ENTITY_CLASSES.size(); i++) {
            Class<?> superclass = MOVEMENT_NOTIFYING_ENTITY_CLASSES.get(i);
            if (superclass.isAssignableFrom(entityClass)) {
                mask |= 1 << i;
            }
        }

        //progress can be lost here, but it can only cost performance
        //copy on write followed by publication in volatile field guarantees visibility of the final state
        Reference2IntOpenHashMap<Class<? extends EntityLike>> copy = CLASS_2_NOTIFY_MASK.clone();
        copy.put(entityClass, mask);
        CLASS_2_NOTIFY_MASK = copy;

        return mask;
    }

}
