package me.jellysquid.mods.lithium.mixin.block.hopper;

import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DoubleInventory.class)
public interface DoubleInventoryAccessor {

    @Accessor("first")
    Inventory getFirst();

    @Accessor("second")
    Inventory getSecond();
}
