package me.jellysquid.mods.lithium.mixin.block.hopper.worldedit_compat;

import me.jellysquid.mods.lithium.common.compat.worldedit.WorldEditCompat;
import me.jellysquid.mods.lithium.common.hopper.UpdateReceiver;
import me.jellysquid.mods.lithium.common.util.DirectionConstants;
import me.jellysquid.mods.lithium.common.world.WorldHelper;
import me.jellysquid.mods.lithium.common.world.blockentity.BlockEntityGetter;
import net.minecraft.block.BlockState;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {

    @Shadow
    public abstract World getWorld();

    @Inject(
            method = "setBlockState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onBlockAdded(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V", shift = At.Shift.BEFORE)
    )
    private void updateHoppersIfWorldEditPresent(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        if (WorldEditCompat.WORLD_EDIT_PRESENT && (state.getBlock() instanceof InventoryProvider || state.hasBlockEntity())) {
            updateHopperCachesOnNewInventoryAdded((WorldChunk) (Object) this, pos, this.getWorld());
        }
    }

    private static void updateHopperCachesOnNewInventoryAdded(WorldChunk worldChunk, BlockPos pos, World world) {
        BlockPos.Mutable neighborPos = new BlockPos.Mutable();
        for (Direction offsetDirection : DirectionConstants.ALL) {
            neighborPos.set(pos, offsetDirection);
            BlockEntity neighborBlockEntity =
                    WorldHelper.arePosWithinSameChunk(pos, neighborPos) ?
                            worldChunk.getBlockEntity(neighborPos, WorldChunk.CreationType.CHECK) :
                            ((BlockEntityGetter) world).getLoadedExistingBlockEntity(neighborPos);
            if (neighborBlockEntity instanceof UpdateReceiver updateReceiver) {
                updateReceiver.invalidateCacheOnNeighborUpdate(offsetDirection.getOpposite());
            }
        }
    }
}
