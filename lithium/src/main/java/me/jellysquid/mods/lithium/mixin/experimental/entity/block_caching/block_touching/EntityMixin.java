package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.block_touching;

import me.jellysquid.mods.lithium.common.block.BlockStateFlagHolder;
import me.jellysquid.mods.lithium.common.block.BlockStateFlags;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCache;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCacheProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * This mixin uses the block caching system to be able to skip entity block interactions when the entity is not a player
 * and the nearby blocks cannot be interacted with by touching them.
 */
@Mixin(Entity.class)
public abstract class EntityMixin implements BlockCacheProvider {
    @Inject(
            method = "checkBlockCollision()V",
            at = @At("HEAD"), cancellable = true
    )
    private void cancelIfSkippable(CallbackInfo ci) {
        //noinspection ConstantConditions
        if (!((Object) this instanceof ServerPlayerEntity)) {
            BlockCache bc = this.getUpdatedBlockCache((Entity)(Object)this);
            if (bc.canSkipBlockTouching()) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "checkBlockCollision()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getX()I", ordinal = 0)
    )
    private void assumeNoTouchableBlock(CallbackInfo ci) {
        BlockCache bc = this.getBlockCache();
        if (bc.isTracking()) {
            bc.setCanSkipBlockTouching(true);
        }
    }

    @Inject(
            method = "checkBlockCollision()V", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onEntityCollision(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V")
    )
    private void checkTouchableBlock(CallbackInfo ci, Box box, BlockPos blockPos, BlockPos blockPos2, BlockPos.Mutable mutable, int i, int j, int k, BlockState blockState) {
        BlockCache bc = this.getBlockCache();
        if (bc.canSkipBlockTouching() &&
                0 != (((BlockStateFlagHolder)blockState).lithium$getAllFlags() & 1 << BlockStateFlags.ENTITY_TOUCHABLE.getIndex())
        ) {
            bc.setCanSkipBlockTouching(false);
        }
    }
}
