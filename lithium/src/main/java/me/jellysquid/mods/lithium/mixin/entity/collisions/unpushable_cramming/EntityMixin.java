package me.jellysquid.mods.lithium.mixin.entity.collisions.unpushable_cramming;

import me.jellysquid.mods.lithium.common.entity.pushable.BlockCachingEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements BlockCachingEntity {
    @Shadow
    private @Nullable BlockState blockStateAtPos;

    @Inject(
            method = "setPos(DDD)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/ChunkSectionPos;getSectionCoord(I)I",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void onPositionChanged(double x, double y, double z, CallbackInfo ci) {
        this.lithiumOnBlockCacheDeleted();
    }

    @Inject(
            method = "baseTick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;hasVehicle()Z",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void onBaseTick(CallbackInfo ci) {
        this.lithiumOnBlockCacheDeleted();
    }

    @Inject(
            method = "getBlockStateAtPos()Lnet/minecraft/block/BlockState;",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
                    shift = At.Shift.AFTER
            )
    )
    private void onBlockCached(CallbackInfoReturnable<BlockState> cir) {
        this.lithiumOnBlockCacheSet(this.blockStateAtPos);
    }

    @Override
    public BlockState getCachedFeetBlockState() {
        return this.blockStateAtPos;
    }
}
