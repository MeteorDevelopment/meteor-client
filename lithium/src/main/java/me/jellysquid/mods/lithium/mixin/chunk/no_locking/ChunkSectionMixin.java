package me.jellysquid.mods.lithium.mixin.chunk.no_locking;

import net.minecraft.block.BlockState;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkSection.class)
public abstract class ChunkSectionMixin {

    @Shadow
    public abstract BlockState setBlockState(int x, int y, int z, BlockState state, boolean lock);

    @Redirect(
            method = "setBlockState(IIILnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkSection;setBlockState(IIILnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;")
    )
    private BlockState setBlockStateNoLocking(ChunkSection chunkSection, int x, int y, int z, BlockState state, boolean lock) {
        return this.setBlockState(x, y, z, state, false);
    }
}
