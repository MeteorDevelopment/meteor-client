package me.jellysquid.mods.lithium.mixin.util.chunk_access;

import me.jellysquid.mods.lithium.common.world.ChunkView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkCache;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin implements ChunkView {

    @Shadow
    protected abstract Chunk getChunk(int chunkX, int chunkZ);

    @Override
    public @Nullable Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ);
    }
}
