package me.jellysquid.mods.lithium.api.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

public interface LithiumDefaultedList {
    /**
     * Call this method when the behavior of
     * {@link net.minecraft.inventory.Inventory#isValid(int, ItemStack)}
     * {@link net.minecraft.inventory.SidedInventory#canInsert(int, ItemStack, Direction)}
     * {@link net.minecraft.inventory.SidedInventory#canExtract(int, ItemStack, Direction)}
     * or similar functionality changed.
     * This method will not need to be called if this change in behavior is triggered by a change of the stack list contents.
     */
    void changedInteractionConditions();
}
