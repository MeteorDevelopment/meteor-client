package me.jellysquid.mods.lithium.mixin.block.hopper;

import me.jellysquid.mods.lithium.common.hopper.UpdateReceiver;
import me.jellysquid.mods.lithium.common.util.DirectionConstants;
import me.jellysquid.mods.lithium.common.world.WorldHelper;
import me.jellysquid.mods.lithium.common.world.blockentity.BlockEntityGetter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(World.class)
public class WorldMixin {

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;onBlockChanged(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;)V"),
            locals = LocalCapture.CAPTURE_FAILHARD

    )
    private void updateHopperOnUpdateSuppression(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir, WorldChunk worldChunk, Block block, BlockState blockState, BlockState blockState2) {
        if ((flags & Block.NOTIFY_NEIGHBORS) == 0) {
            //No block updates were sent. We need to update nearby hoppers to avoid outdated inventory caches being used

            //Small performance improvement when getting block entities within the same chunk.
            Map<BlockPos, BlockEntity> blockEntities = WorldHelper.areNeighborsWithinSameChunk(pos) ? worldChunk.getBlockEntities() : null;
            if (blockState != blockState2 && (blockEntities == null || !blockEntities.isEmpty())) {
                for (Direction direction : DirectionConstants.ALL) {
                    BlockPos offsetPos = pos.offset(direction);
                    //Directly get the block entity instead of getting the block state first. Maybe that is faster, maybe not.
                    BlockEntity hopper = blockEntities != null ? blockEntities.get(offsetPos) : ((BlockEntityGetter) this).getLoadedExistingBlockEntity(offsetPos);
                    if (hopper instanceof UpdateReceiver updateReceiver) {
                        updateReceiver.invalidateCacheOnNeighborUpdate(direction == Direction.DOWN);
                    }
                }
            }
        }
    }
}
