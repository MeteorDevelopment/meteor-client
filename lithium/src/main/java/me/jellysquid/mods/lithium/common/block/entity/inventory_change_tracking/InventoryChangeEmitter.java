package me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking;

import me.jellysquid.mods.lithium.common.hopper.LithiumStackList;

/**
 * Interface for Objects that can emit various inventory change events. This does not mean that the inventory
 * creates those events - this requirement is met by InventoryChangeTracker. This distinction is needed due
 * to modded inventories being able to inherit from LockableContainerBlockEntity, which does not guarantee the creation
 * of the required events but implements most of the inventory change listening.
 * The forwarding methods below are helpers, it is not recommended to call them from outside InventoryChangeTracker.java
 */
public interface InventoryChangeEmitter {
    void emitStackListReplaced();

    void emitRemoved();

    void emitContentModified();

    void emitFirstComparatorAdded();

    void forwardContentChangeOnce(InventoryChangeListener inventoryChangeListener, LithiumStackList stackList, InventoryChangeTracker thisTracker);

    void forwardMajorInventoryChanges(InventoryChangeListener inventoryChangeListener);

    void stopForwardingMajorInventoryChanges(InventoryChangeListener inventoryChangeListener);

    default void emitCallbackReplaced() {
        this.emitRemoved();
    }
}
