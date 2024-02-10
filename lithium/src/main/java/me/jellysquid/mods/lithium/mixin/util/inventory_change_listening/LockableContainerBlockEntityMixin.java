package me.jellysquid.mods.lithium.mixin.util.inventory_change_listening;


import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import me.jellysquid.mods.lithium.api.inventory.LithiumInventory;
import me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeEmitter;
import me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import me.jellysquid.mods.lithium.common.hopper.InventoryHelper;
import me.jellysquid.mods.lithium.common.hopper.LithiumStackList;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LockableContainerBlockEntity.class)
public abstract class LockableContainerBlockEntityMixin implements InventoryChangeEmitter, Inventory {
    ReferenceArraySet<InventoryChangeListener> inventoryChangeListeners = null;
    ReferenceArraySet<InventoryChangeListener> inventoryHandlingTypeListeners = null;

    @Override
    public void emitContentModified() {
        ReferenceArraySet<InventoryChangeListener> inventoryChangeListeners = this.inventoryChangeListeners;
        if (inventoryChangeListeners != null) {
            for (InventoryChangeListener inventoryChangeListener : inventoryChangeListeners) {
                inventoryChangeListener.handleInventoryContentModified(this);
            }
            inventoryChangeListeners.clear();
        }
    }

    @Override
    public void emitStackListReplaced() {
        ReferenceArraySet<InventoryChangeListener> listeners = this.inventoryHandlingTypeListeners;
        if (listeners != null && !listeners.isEmpty()) {
            for (InventoryChangeListener inventoryChangeListener : listeners) {
                inventoryChangeListener.handleStackListReplaced(this);
            }
            listeners.clear();
        }

        if (this instanceof InventoryChangeListener listener) {
            listener.handleStackListReplaced(this);
        }

        this.invalidateChangeListening();
    }

    @Override
    public void emitRemoved() {
        ReferenceArraySet<InventoryChangeListener> listeners = this.inventoryHandlingTypeListeners;
        if (listeners != null && !listeners.isEmpty()) {
            for (InventoryChangeListener listener : listeners) {
                listener.handleInventoryRemoved(this);
            }
            listeners.clear();
        }

        if (this instanceof InventoryChangeListener listener) {
            listener.handleInventoryRemoved(this);
        }

        this.invalidateChangeListening();
    }

    private void invalidateChangeListening() {
        if (this.inventoryChangeListeners != null) {
            this.inventoryChangeListeners.clear();
        }

        LithiumStackList lithiumStackList = this instanceof LithiumInventory ? InventoryHelper.getLithiumStackListOrNull((LithiumInventory) this) : null;
        if (lithiumStackList != null && this instanceof InventoryChangeTracker inventoryChangeTracker) {
            lithiumStackList.removeInventoryModificationCallback(inventoryChangeTracker);
        }
    }

    @Override
    public void emitFirstComparatorAdded() {
        ReferenceArraySet<InventoryChangeListener> inventoryChangeListeners = this.inventoryChangeListeners;
        if (inventoryChangeListeners != null && !inventoryChangeListeners.isEmpty()) {
            inventoryChangeListeners.removeIf(inventoryChangeListener -> inventoryChangeListener.handleComparatorAdded(this));
        }
    }

    @Override
    public void forwardContentChangeOnce(InventoryChangeListener inventoryChangeListener, LithiumStackList stackList, InventoryChangeTracker thisTracker) {
        if (this.inventoryChangeListeners == null) {
            this.inventoryChangeListeners = new ReferenceArraySet<>(1);
        }
        stackList.setInventoryModificationCallback(thisTracker);
        this.inventoryChangeListeners.add(inventoryChangeListener);

    }

    @Override
    public void forwardMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        if (this.inventoryHandlingTypeListeners == null) {
            this.inventoryHandlingTypeListeners = new ReferenceArraySet<>(1);
        }
        this.inventoryHandlingTypeListeners.add(inventoryChangeListener);
    }

    @Override
    public void stopForwardingMajorInventoryChanges(InventoryChangeListener inventoryChangeListener) {
        if (this.inventoryHandlingTypeListeners != null) {
            this.inventoryHandlingTypeListeners.remove(inventoryChangeListener);
        }
    }
}
