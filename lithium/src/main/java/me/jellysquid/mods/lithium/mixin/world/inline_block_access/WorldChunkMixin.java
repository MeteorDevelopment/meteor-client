package me.jellysquid.mods.lithium.mixin.world.inline_block_access;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = WorldChunk.class, priority = 500)
public abstract class WorldChunkMixin extends Chunk {
    private static final BlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();
    private static final FluidState DEFAULT_FLUID_STATE = Fluids.EMPTY.getDefaultState();

    public WorldChunkMixin(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biome, long inhabitedTime, @Nullable ChunkSection[] sectionArrayInitializer, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biome, inhabitedTime, sectionArrayInitializer, blendingData);
    }

    /**
     * @reason Reduce method size to help the JVM inline
     * @author JellySquid
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int chunkY = this.getSectionIndex(y);
        ChunkSection[] sectionArray = this.getSectionArray();
        if (chunkY >= 0 && chunkY < sectionArray.length) {
            ChunkSection section = sectionArray[chunkY];

            //checking isEmpty cannot be skipped here. https://bugs.mojang.com/browse/MC-232360
            // Chunk Sections that only contain air and cave_air are treated as empty
            if (!section.isEmpty()) {
                return section.getBlockState(x & 15, y & 15, z & 15);
            }
        }

        return DEFAULT_BLOCK_STATE;
    }

    /**
     * @reason Reduce method size to help the JVM inline
     * @author JellySquid, Maity
     */
    @Overwrite
    public FluidState getFluidState(int x, int y, int z) {
        int chunkY = this.getSectionIndex(y);
        ChunkSection[] sectionArray = this.getSectionArray();
        if (chunkY >= 0 && chunkY < sectionArray.length) {
            ChunkSection section = sectionArray[chunkY];
            return section.getFluidState(x & 15, y & 15, z & 15);
        }

        return DEFAULT_FLUID_STATE;
    }
}
