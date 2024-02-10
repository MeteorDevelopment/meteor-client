package me.jellysquid.mods.lithium.common.world.chunk;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.chunk.Chunk;

import java.util.concurrent.CompletableFuture;

public interface ChunkHolderExtended {
    /**
     * @return The existing future for the status at ordinal {@param index} or null if none exists
     */
    CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getFutureByStatus(int index);

    /**
     * Updates the future for the status at ordinal {@param index}.
     */
    void setFutureForStatus(int index, CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future);

    /**
     * Updates the last accessed timestamp for this chunk. This is used to determine if a ticket was recently
     * created for it.
     *
     * @param time The current time
     * @return True if the chunk needs a new ticket to be created in order to retain it, otherwise false
     */
    boolean updateLastAccessTime(long time);
}
