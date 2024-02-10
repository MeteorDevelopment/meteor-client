package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.support_cache;

import me.jellysquid.mods.lithium.common.world.blockentity.SupportCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(targets = "net.minecraft.world.chunk.WorldChunk$DirectBlockEntityTickInvoker")
public class DirectBlockEntityTickInvokerMixin<T extends BlockEntity> {

    @Shadow
    @Final
    private T blockEntity;

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/WorldChunk;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/util/function/Supplier;)V"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;)V")
            )
    )
    private BlockState getCachedState(WorldChunk chunk, BlockPos pos) {
        return this.blockEntity.getCachedState();
    }

    @Redirect(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/BlockEntityType;supports(Lnet/minecraft/block/BlockState;)Z"
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/util/function/Supplier;)V"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/BlockEntity;)V")
            )
    )
    private boolean cachedIsSupported(BlockEntityType<?> blockEntityType, BlockState block) {
        return ((SupportCache) this.blockEntity).isSupported();
    }

}
