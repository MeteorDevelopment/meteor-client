package me.jellysquid.mods.lithium.mixin.block.hopper;

import me.jellysquid.mods.lithium.api.inventory.LithiumCooldownReceivingInventory;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Inventory.class)
public interface InventoryMixin extends LithiumCooldownReceivingInventory {
}
