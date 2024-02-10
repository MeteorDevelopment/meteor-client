package me.jellysquid.mods.lithium.mixin.block.hopper;

import me.jellysquid.mods.lithium.api.inventory.LithiumInventory;
import net.minecraft.block.entity.*;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public class InventoryAccessors {
    @Mixin(AbstractFurnaceBlockEntity.class)
    public abstract static class InventoryAccessorAbstractFurnaceBlockEntity implements LithiumInventory {
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

    @Mixin(BarrelBlockEntity.class)
    public abstract static class InventoryAccessorBarrelBlockEntity implements LithiumInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

    @Mixin(BrewingStandBlockEntity.class)
    public abstract static class InventoryAccessorBrewingStandBlockEntity implements LithiumInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

    @Mixin(ChestBlockEntity.class)
    public abstract static class InventoryAccessorChestBlockEntity implements LithiumInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

    @Mixin(DispenserBlockEntity.class)
    public abstract static class InventoryAccessorDispenserBlockEntity implements LithiumInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

    @Mixin(HopperBlockEntity.class)
    public abstract static class InventoryAccessorHopperBlockEntity implements LithiumInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

    @Mixin(ShulkerBoxBlockEntity.class)
    public abstract static class InventoryAccessorShulkerBoxBlockEntity implements LithiumInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

    @Mixin(StorageMinecartEntity.class)
    public abstract static class InventoryAccessorStorageMinecartEntity implements LithiumInventory {
        @Override
        @Accessor("inventory")
        public abstract DefaultedList<ItemStack> getInventoryLithium();

        @Override
        @Accessor("inventory")
        public abstract void setInventoryLithium(DefaultedList<ItemStack> inventory);
    }

}
