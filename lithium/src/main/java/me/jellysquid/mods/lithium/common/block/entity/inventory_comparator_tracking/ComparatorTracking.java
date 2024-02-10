package me.jellysquid.mods.lithium.common.block.entity.inventory_comparator_tracking;

import me.jellysquid.mods.lithium.common.util.DirectionConstants;
import me.jellysquid.mods.lithium.common.world.blockentity.BlockEntityGetter;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class ComparatorTracking {

    public static void notifyNearbyBlockEntitiesAboutNewComparator(World world, BlockPos pos) {
        BlockPos.Mutable searchPos = new BlockPos.Mutable();
        for (Direction searchDirection : DirectionConstants.HORIZONTAL) {
            for (int searchOffset = 1; searchOffset <= 2; searchOffset++) {
                searchPos.set(pos);
                searchPos.move(searchDirection, searchOffset);
                BlockState blockState = world.getBlockState(searchPos);
                if (blockState.getBlock() instanceof BlockEntityProvider) {
                    BlockEntity blockEntity = ((BlockEntityGetter) world).getLoadedExistingBlockEntity(searchPos);
                    if (blockEntity instanceof Inventory && blockEntity instanceof ComparatorTracker comparatorTracker) {
                        comparatorTracker.onComparatorAdded(searchDirection, searchOffset);
                    }
                }
            }
        }
    }

    public static boolean findNearbyComparators(World world, BlockPos pos) {
        BlockPos.Mutable searchPos = new BlockPos.Mutable();
        for (Direction searchDirection : DirectionConstants.HORIZONTAL) {
            for (int searchOffset = 1; searchOffset <= 2; searchOffset++) {
                searchPos.set(pos);
                searchPos.move(searchDirection, searchOffset);
                BlockState blockState = world.getBlockState(searchPos);
                if (blockState.isOf(Blocks.COMPARATOR)) {
                    return true;
                }
            }
        }
        return false;
    }
}
