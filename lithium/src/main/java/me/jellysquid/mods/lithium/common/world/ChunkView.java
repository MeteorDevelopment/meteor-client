package me.jellysquid.mods.lithium.common.world;

import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

public interface ChunkView {

    @Nullable
    Chunk getLoadedChunk(int chunkX, int chunkZ);
}
