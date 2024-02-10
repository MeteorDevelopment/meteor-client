package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching;

import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCache;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCacheProvider;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin implements BlockCacheProvider {
    private final BlockCache blockCache = new BlockCache();

    @Override
    public BlockCache getBlockCache() {
        return blockCache;
    }

    @Inject(
            method = "remove",
            at = @At("HEAD")
    )
    private void removeBlockCache(Entity.RemovalReason reason, CallbackInfo ci) {
        this.blockCache.remove();
    }
}
