package me.jellysquid.mods.lithium.mixin.experimental.entity.block_caching.block_support;

import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCache;
import me.jellysquid.mods.lithium.common.entity.block_tracking.BlockCacheProvider;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements BlockCacheProvider {
    @Inject(
            method = "updateSupportingBlockPos", cancellable = true,
            at = @At(
                    value = "INVOKE", shift = At.Shift.BEFORE,
                    target = "Lnet/minecraft/entity/Entity;getBoundingBox()Lnet/minecraft/util/math/Box;"
            )
    )
    private void cancelIfSkippable(boolean onGround, Vec3d movement, CallbackInfo ci) {
        if (movement == null || (movement.x == 0 && movement.z == 0)) {
            //noinspection ConstantConditions
            BlockCache bc = this.getUpdatedBlockCache((Entity) (Object) this);
            if (bc.canSkipSupportingBlockSearch()) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "updateSupportingBlockPos",
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/World;findSupportingBlockPos(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/Optional;")
    )
    private void cacheSupportingBlockSearch(CallbackInfo ci) {
        BlockCache bc = this.getBlockCache();
        if (bc.isTracking()) {
            bc.setCanSkipSupportingBlockSearch(true);
        }
    }

    @Inject(
            method = "updateSupportingBlockPos",
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/World;findSupportingBlockPos(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Box;)Ljava/util/Optional;")
    )
    private void uncacheSupportingBlockSearch(CallbackInfo ci) {
        BlockCache bc = this.getBlockCache();
        if (bc.isTracking()) {
            bc.setCanSkipSupportingBlockSearch(false);
        }
    }

    @Inject(
            method = "updateSupportingBlockPos",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;empty()Ljava/util/Optional;", remap = false)
    )
    private void uncacheSupportingBlockSearch1(boolean onGround, Vec3d movement, CallbackInfo ci) {
        BlockCache bc = this.getBlockCache();
        if (bc.isTracking()) {
            bc.setCanSkipSupportingBlockSearch(false);
        }
    }
}
