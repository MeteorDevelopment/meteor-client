package me.jellysquid.mods.lithium.mixin.world.chunk_access;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Implement the interface members of {@link WorldView} and {@link CollisionView} directly to avoid complicated
 * method invocations between interface boundaries, helping the JVM to inline and optimize code.
 */
@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess {
    /**
     * @reason Remove dynamic-dispatch and inline call
     * @author JellySquid
     */
    @Overwrite
    public WorldChunk getWorldChunk(BlockPos pos) {
        return (WorldChunk) this.getChunk(pos);
    }

    @Override
    public Chunk getChunk(BlockPos pos) {
        return this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, true);
    }

    /**
     * @reason Remove dynamic-dispatch and inline call
     * @author JellySquid
     */
    @Override
    @Overwrite
    public WorldChunk getChunk(int chunkX, int chunkZ) {
        return (WorldChunk) this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
    }

    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status) {
        return this.getChunk(chunkX, chunkZ, status, true);
    }

    @Override
    public BlockView getChunkAsView(int chunkX, int chunkZ) {
        return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }
}
