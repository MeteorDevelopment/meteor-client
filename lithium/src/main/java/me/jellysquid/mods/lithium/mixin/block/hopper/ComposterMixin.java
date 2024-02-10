package me.jellysquid.mods.lithium.mixin.block.hopper;

import me.jellysquid.mods.lithium.common.hopper.BlockStateOnlyInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ComposterMixin {

    @Mixin(targets = "net.minecraft.block.ComposterBlock$ComposterInventory")
    static abstract class ComposterBlockComposterInventoryMixin implements BlockStateOnlyInventory {
        @Shadow
        private boolean dirty;

        /**
         * Fixes composter inventories becoming blocked forever for no reason, which makes them not cacheable.
         */
        @Inject(
                method = "markDirty()V",
                at = @At(
                        value = "INVOKE",
                        target = "Lnet/minecraft/block/ComposterBlock$ComposterInventory;removeStack(I)Lnet/minecraft/item/ItemStack;"
                )
        )
        private void resetDirty(CallbackInfo ci) {
            this.dirty = false;
        }

    }

    @Mixin(targets = "net.minecraft.block.ComposterBlock$DummyInventory")
    static abstract class ComposterBlockDummyInventoryMixin implements BlockStateOnlyInventory {

    }

    @Mixin(targets = "net.minecraft.block.ComposterBlock$FullComposterInventory")
    static abstract class ComposterBlockFullComposterInventoryMixin implements BlockStateOnlyInventory {

    }
}
