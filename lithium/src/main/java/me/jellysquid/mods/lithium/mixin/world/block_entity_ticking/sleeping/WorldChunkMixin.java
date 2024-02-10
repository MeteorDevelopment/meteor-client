package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.sleeping;

import me.jellysquid.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {

    @Inject(
            method = "method_31719",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addBlockEntityTicker(Lnet/minecraft/world/chunk/BlockEntityTickInvoker;)V" ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void setBlockEntityTickingOrder(BlockEntity blockEntity, BlockEntityTicker<?> blockEntityTicker, BlockPos pos, @Coerce Object wrappedBlockEntityTickInvoker, CallbackInfoReturnable<?> cir, BlockEntityTickInvoker blockEntityTickInvoker, @Coerce Object wrappedBlockEntityTickInvoker2) {
        if (blockEntity instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.setTickWrapper((WrappedBlockEntityTickInvokerAccessor) wrappedBlockEntityTickInvoker2);
        }
    }

    @Inject(
            method = "method_31719",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk$WrappedBlockEntityTickInvoker;setWrapped(Lnet/minecraft/world/chunk/BlockEntityTickInvoker;)V" ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void setBlockEntityTickingOrder(BlockEntity blockEntity, BlockEntityTicker<?> blockEntityTicker, BlockPos pos, @Coerce Object wrappedBlockEntityTickInvoker, CallbackInfoReturnable<?> cir, BlockEntityTickInvoker blockEntityTickInvoker) {
        if (blockEntity instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.setTickWrapper((WrappedBlockEntityTickInvokerAccessor) wrappedBlockEntityTickInvoker);
        }
    }

}
