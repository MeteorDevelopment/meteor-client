package me.jellysquid.mods.lithium.api.inventory;

import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Provides the ability for mods to allow Lithium's hopper optimizations to access their inventories' for item transfers.
 * This exists because Lithium's optimized hopper logic will only interact with inventories more efficiently than
 * vanilla if the stack list can be directly accessed and replaced with Lithium's custom stack list.
 * It is not required to implement this interface, but doing so will allow the mod's inventories to benefit from
 * Lithium's optimizations.
 * <p>
 * This interface should be implemented by your {@link net.minecraft.inventory.Inventory} or
 * {@link net.minecraft.inventory.SidedInventory} type to access the stack list.
 * <p>
 * An inventory must not extend {@link net.minecraft.block.entity.BlockEntity} if it has a supporting block that
 * implements {@link net.minecraft.block.InventoryProvider}.
 * <p>
 * The hopper interaction behavior of a LithiumInventory should only change if the content of the inventory
 * stack list also changes. For example, an inventory which only accepts an item if it already contains an item of the
 * same type would work fine (changing the acceptance condition only happens when changing the inventory contents here).
 * However, an inventory which accepts an item only if a certain block is near its position will need to signal this
 * change to hoppers by calling {@link LithiumDefaultedList#changedInteractionConditions()}.
 *
 * @author 2No2Name
 */
public interface LithiumInventory extends Inventory {

    /**
     * Getter for the inventory stack list of this inventory.
     *
     * @return inventory stack list
     */
    DefaultedList<ItemStack> getInventoryLithium();

    /**
     * Setter for the inventory stack list of this inventory.
     * Used to replace the stack list with Lithium's custom stack list.
     *
     * @param inventory inventory stack list
     */
    void setInventoryLithium(DefaultedList<ItemStack> inventory);

    /**
     * Generates the loot like a hopper access would do in vanilla.
     * <p>
     * If a modded inventory has custom loot generation code, it will be required to override this
     * loot generation method. Otherwise, its loot may be generated too late.
     */
    default void generateLootLithium() {
        if (this instanceof LootableContainerBlockEntity) {
            ((LootableContainerBlockEntity) this).generateLoot(null);
        }
        if (this instanceof VehicleInventory) {
            ((VehicleInventory) this).generateInventoryLoot(null);
        }
    }
}
