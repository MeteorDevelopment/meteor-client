package me.jellysquid.mods.lithium.mixin.world.inline_block_access;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public abstract class WorldMixin implements HeightLimitView {
    private static final BlockState OUTSIDE_WORLD_BLOCK = Blocks.VOID_AIR.getDefaultState();
    private static final BlockState INSIDE_WORLD_DEFAULT_BLOCK = Blocks.AIR.getDefaultState();

    @Shadow
    public abstract WorldChunk getChunk(int i, int j);

    /**
     * @reason Reduce method size to help the JVM inline, Avoid excess height limit checks
     * @author 2No2Name
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        WorldChunk worldChunk = this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
        ChunkSection[] sections = worldChunk.getSectionArray();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int chunkY = this.getSectionIndex(y);
        if (chunkY < 0 || chunkY >= sections.length) {
            return OUTSIDE_WORLD_BLOCK;
        }

        ChunkSection section = sections[chunkY];
        if (section == null || section.isEmpty()) {
            return INSIDE_WORLD_DEFAULT_BLOCK;
        }
        return section.getBlockState(x & 15, y & 15, z & 15);
        //This code path is slower than with the extra world height limit check. Tradeoff in favor of the default path.
    }

    @Redirect(
            method = "getFluidState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;isOutOfHeightLimit(Lnet/minecraft/util/math/BlockPos;)Z"
            )
    )
    private boolean skipFluidHeightLimitTest(World world, BlockPos pos) {
        return world.isOutOfHeightLimit(pos);
    }
}
