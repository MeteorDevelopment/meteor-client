package me.jellysquid.mods.lithium.mixin.util.inventory_comparator_tracking;

import me.jellysquid.mods.lithium.common.block.entity.inventory_comparator_tracking.ComparatorTracking;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ComparatorBlock.class)
public abstract class ComparatorBlockMixin extends AbstractRedstoneGateBlock {
    protected ComparatorBlockMixin(Settings settings) {
        super(settings);
    }

    @Intrinsic
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
    }

    @SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
    @Inject(
            method = "onBlockAdded(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V",
            at = @At("RETURN")
    )
    private void notifyOnBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        if (!oldState.isOf(Blocks.COMPARATOR)) {
            ComparatorTracking.notifyNearbyBlockEntitiesAboutNewComparator(world, pos);
        }
    }
}
