package me.jellysquid.mods.lithium.mixin.world.chunk_ticking.spread_ice;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Biome.class)
public class BiomeMixin {

    @Redirect(
            method = "canSetIce(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldView;getFluidState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/fluid/FluidState;"
            )
    )
    private FluidState getNull(WorldView instance, BlockPos blockPos) {
        return null;
    }

    @Redirect(
            method = "canSetIce(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/fluid/FluidState;getFluid()Lnet/minecraft/fluid/Fluid;"
            )
    )
    private Fluid skipFluidCheck(FluidState fluidState) {
        return Fluids.WATER;
    }

    @Redirect(
            method = "canSetIce(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;Z)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"
            )
    )
    private Block fluidCheckAndGetBlock(BlockState blockState) {
        return blockState.getFluidState().getFluid() == Fluids.WATER ? blockState.getBlock() : null;
    }
}
