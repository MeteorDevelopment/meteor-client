package me.jellysquid.mods.lithium.mixin.world.block_entity_ticking.support_cache;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Fix {@link net.minecraft.block.AbstractBlock.AbstractBlockState#onBlockAdded(World, BlockPos, BlockState, boolean)}
 * being able to change the blockState but the blockEntity's cached state still being set to the old blockState.
 * This only affects hoppers, as hoppers are the only block with a blockentity that also implements onBlockAdded.
 *
 * @author 2No2name
 */
@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin {

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Redirect(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockEntityProvider;createBlockEntity(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/entity/BlockEntity;"
            )
    )
    private BlockEntity createBlockEntityWithCachedStateFix(BlockEntityProvider blockEntityProvider, BlockPos pos, BlockState state) {
        return blockEntityProvider.createBlockEntity(pos, this.getBlockState(pos));
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/entity/BlockEntity;setCachedState(Lnet/minecraft/block/BlockState;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void fixCachedState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir, int i, ChunkSection chunkSection, boolean bl, int j, int k, int l, BlockState blockState, Block block, BlockEntity blockEntity) {
        BlockState blockState1 = this.getBlockState(pos);
        if (blockState1 != state) {
            //noinspection deprecation
            blockEntity.setCachedState(blockState1);
        }
    }
}
