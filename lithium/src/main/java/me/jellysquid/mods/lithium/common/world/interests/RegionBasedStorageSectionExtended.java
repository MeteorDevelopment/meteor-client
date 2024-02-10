package me.jellysquid.mods.lithium.common.world.interests;

import java.util.stream.Stream;

public interface RegionBasedStorageSectionExtended<R> {
    /**
     * Fast-path for retrieving all items in a chunk column. This avoids needing to retrieve items for each sub-chunk
     * individually.
     *
     * @param chunkX The x-coordinate of the chunk column
     * @param chunkZ The z-coordinate of the chunk column
     */
    Stream<R> getWithinChunkColumn(int chunkX, int chunkZ);

    /**
     * Fast-path for collecting all items in a chunk column. This avoids needing to retrieve items for each sub-chunk
     * individually.
     *
     * @param chunkX The x-coordinate of the chunk column
     * @param chunkZ The z-coordinate of the chunk column
     */
    Iterable<R> getInChunkColumn(int chunkX, int chunkZ);
}
