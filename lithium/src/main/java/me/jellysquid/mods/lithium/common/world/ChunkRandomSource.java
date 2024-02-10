package me.jellysquid.mods.lithium.common.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ChunkRandomSource {
    /**
     * Alternative implementation of {@link World#getRandomPosInChunk(int, int, int, int)} which does not allocate
     * a new {@link BlockPos}.
     */
    void getRandomPosInChunk(int x, int y, int z, int mask, BlockPos.Mutable out);
}
