package me.jellysquid.mods.lithium.mixin.util.inventory_change_listening;

import me.jellysquid.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.minecraft.block.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class StackListReplacementTracking {

    @Mixin(AbstractFurnaceBlockEntity.class)
    public abstract static class InventoryChangeTrackingAbstractFurnaceBlockEntity implements InventoryChangeTracker {
        //Handled in LockableConainerBlockEntity
    }

    @Mixin(BrewingStandBlockEntity.class)
    public abstract static class InventoryChangeTrackingBrewingStandBlockEntity implements InventoryChangeTracker {
        //Handled in LockableConainerBlockEntity
    }

    @Mixin(LockableContainerBlockEntity.class)
    public abstract static class StackListReplacementTrackingLockableContainerBlockEntity {
        @Inject(method = "readNbt", at = @At("RETURN" ))
        public void readNbtStackListReplacement(NbtCompound nbt, CallbackInfo ci) {
            if (this instanceof InventoryChangeTracker inventoryChangeTracker) {
                inventoryChangeTracker.emitStackListReplaced();
            }
        }
    }

    @Mixin(BarrelBlockEntity.class)
    public abstract static class InventoryChangeTrackingBarrelBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setInvStackList", at = @At("RETURN" ))
        public void setInventoryStackListReplacement(DefaultedList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(ChestBlockEntity.class)
    public abstract static class InventoryChangeTrackingChestBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setInvStackList", at = @At("RETURN" ))
        public void setInventoryStackListReplacement(DefaultedList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(DispenserBlockEntity.class)
    public abstract static class InventoryChangeTrackingDispenserBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setInvStackList", at = @At("RETURN" ))
        public void setInventoryStackListReplacement(DefaultedList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(HopperBlockEntity.class)
    public abstract static class InventoryChangeTrackingHopperBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setInvStackList", at = @At("RETURN" ))
        public void setInventoryStackListReplacement(DefaultedList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }

    @Mixin(ShulkerBoxBlockEntity.class)
    public abstract static class InventoryChangeTrackingShulkerBoxBlockEntity implements InventoryChangeTracker {
        @Inject(method = "setInvStackList", at = @At("RETURN" ))
        public void setInventoryStackListReplacement(DefaultedList<ItemStack> list, CallbackInfo ci) {
            this.emitStackListReplaced();
        }
    }
}
