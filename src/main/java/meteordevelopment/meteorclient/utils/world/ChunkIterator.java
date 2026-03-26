/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import meteordevelopment.meteorclient.mixin.ClientChunkMapAccessor;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.lang.reflect.Field;
import java.util.Iterator;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ChunkIterator implements Iterator<ChunkAccess> {
    private static final Field STORAGE_FIELD = getStorageField();

    private final ClientChunkMapAccessor map;
    private final boolean onlyWithLoadedNeighbours;

    private int i = 0;
    private ChunkAccess chunk;

    public ChunkIterator(boolean onlyWithLoadedNeighbours) {
        this.onlyWithLoadedNeighbours = onlyWithLoadedNeighbours;
        this.map = getChunkMap();

        getNext();
    }

    private static Field getStorageField() {
        try {
            Field field = ClientChunkCache.class.getDeclaredField("storage");
            field.setAccessible(true);
            return field;
        }
        catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to resolve ClientChunkCache storage field.", e);
        }
    }

    private static ClientChunkMapAccessor getChunkMap() {
        try {
            return (ClientChunkMapAccessor) STORAGE_FIELD.get(mc.level.getChunkSource());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access loaded client chunks.", e);
        }
    }

    private ChunkAccess getNext() {
        ChunkAccess prev = chunk;
        chunk = null;

        if (map == null) return prev;

        while (i < map.meteor$getChunks().length()) {
            chunk = map.meteor$getChunks().get(i++);
            if (chunk != null && (!onlyWithLoadedNeighbours || isInRadius(chunk))) break;
        }

        return prev;
    }

    private boolean isInRadius(ChunkAccess chunk) {
        int x = chunk.getPos().x();
        int z = chunk.getPos().z();

        return mc.level.getChunkSource().hasChunk(x + 1, z) && mc.level.getChunkSource().hasChunk(x - 1, z) && mc.level.getChunkSource().hasChunk(x, z + 1) && mc.level.getChunkSource().hasChunk(x, z - 1);
    }

    @Override
    public boolean hasNext() {
        return chunk != null;
    }

    @Override
    public ChunkAccess next() {
        return getNext();
    }
}
