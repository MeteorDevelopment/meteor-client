package me.jellysquid.mods.lithium.common.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

public record SleepUntilTimeBlockEntityTickInvoker(BlockEntity sleepingBlockEntity, long sleepUntilTickExclusive,
                                                   BlockEntityTickInvoker delegate) implements BlockEntityTickInvoker {

    @Override
    public void tick() {
        //noinspection ConstantConditions
        long tickTime = this.sleepingBlockEntity.getWorld().getTime();
        if (tickTime >= this.sleepUntilTickExclusive) {
            ((SleepingBlockEntity) this.sleepingBlockEntity).setTicker(this.delegate);
            this.delegate.tick();
        }
    }

    @Override
    public boolean isRemoved() {
        return this.sleepingBlockEntity.isRemoved();
    }

    @Override
    public BlockPos getPos() {
        return this.sleepingBlockEntity.getPos();
    }

    @Override
    public String getName() {
        //noinspection ConstantConditions
        return BlockEntityType.getId(this.sleepingBlockEntity.getType()).toString();
    }
}
