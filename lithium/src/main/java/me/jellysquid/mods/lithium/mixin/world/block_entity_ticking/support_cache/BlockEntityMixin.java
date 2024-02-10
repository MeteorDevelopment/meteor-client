package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.support_cache;

import me.jellysquid.mods.lithium.common.world.blockentity.SupportCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements SupportCache {
    @Shadow
    public abstract BlockEntityType<?> getType();

    private boolean supportTestResult;

    @Inject(method = "<init>(Lnet/minecraft/block/entity/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("RETURN"))
    private void initSupportCache(BlockEntityType<?> type, BlockPos pos, BlockState cachedState, CallbackInfo ci) {
        this.supportTestResult = this.getType().supports(cachedState);
    }

    @Inject(method = "setCachedState(Lnet/minecraft/block/BlockState;)V", at = @At("RETURN"))
    private void updateSupportCache(BlockState cachedState, CallbackInfo ci) {
        this.supportTestResult = this.getType().supports(cachedState);
    }

    @Override
    public boolean isSupported() {
        return this.supportTestResult;
    }
}
