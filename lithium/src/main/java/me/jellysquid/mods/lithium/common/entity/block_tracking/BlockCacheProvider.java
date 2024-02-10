package me.jellysquid.mods.lithium.common.entity.block_tracking;

import net.minecraft.entity.Entity;

public interface BlockCacheProvider {
    BlockCache getBlockCache();

    default BlockCache getUpdatedBlockCache(Entity entity) {
        BlockCache bc = this.getBlockCache();
        bc.updateCache(entity);
        return bc;
    }
}
