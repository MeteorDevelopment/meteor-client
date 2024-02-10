package me.jellysquid.mods.lithium.mixin.alloc.composter;

import me.jellysquid.mods.lithium.common.util.ArrayConstants;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

public class ComposterMixin {

    @Mixin(targets = "net.minecraft.block.ComposterBlock$ComposterInventory")
    static abstract class ComposterBlockComposterInventoryMixin implements SidedInventory {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getAvailableSlots(Direction side) {
            return side == Direction.UP ? ArrayConstants.ZERO : ArrayConstants.EMPTY;
        }
    }

    @Mixin(targets = "net.minecraft.block.ComposterBlock$DummyInventory")
    static abstract class ComposterBlockDummyInventoryMixin implements SidedInventory {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getAvailableSlots(Direction side) {
            return ArrayConstants.EMPTY;
        }
    }

    @Mixin(targets = "net.minecraft.block.ComposterBlock$FullComposterInventory")
    static abstract class ComposterBlockFullComposterInventoryMixin implements SidedInventory {
        /**
         * @author 2No2Name
         * @reason avoid allocation
         */
        @Overwrite
        public int[] getAvailableSlots(Direction side) {
            return side == Direction.DOWN ? ArrayConstants.ZERO : ArrayConstants.EMPTY;
        }
    }
}
