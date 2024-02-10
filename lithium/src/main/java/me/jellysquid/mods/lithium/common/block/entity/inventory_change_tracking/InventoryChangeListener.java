package me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking;

import net.minecraft.inventory.Inventory;

public interface InventoryChangeListener {
    default void handleStackListReplaced(Inventory inventory) {
        this.handleInventoryRemoved(inventory);
    }

    void handleInventoryContentModified(Inventory inventory);

    void handleInventoryRemoved(Inventory inventory);

    boolean handleComparatorAdded(Inventory inventory);
}
