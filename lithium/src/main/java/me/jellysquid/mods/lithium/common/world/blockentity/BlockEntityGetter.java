package me.jellysquid.mods.lithium.common.world.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public interface BlockEntityGetter {
    BlockEntity getLoadedExistingBlockEntity(BlockPos pos);
}
