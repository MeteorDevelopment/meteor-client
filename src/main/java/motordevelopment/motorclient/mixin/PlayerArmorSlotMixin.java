/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.mixin;

import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net/minecraft/screen/PlayerScreenHandler$1")
public abstract class PlayerArmorSlotMixin extends Slot {
    public PlayerArmorSlotMixin(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public int getMaxItemCount() {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) return 64;
        return super.getMaxItemCount();
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) return true;
        return super.canInsert(stack);
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerEntity) {
        if (Modules.get().get(InventoryTweaks.class).armorStorage()) return true;
        return super.canTakeItems(playerEntity);
    }
}
