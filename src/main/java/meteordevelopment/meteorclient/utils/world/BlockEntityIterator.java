/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import meteordevelopment.meteorclient.mixin.ChunkAccessAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Iterator;
import java.util.Map;

public class BlockEntityIterator implements Iterator<BlockEntity> {
    private final Iterator<ChunkAccess> chunks;
    private Iterator<BlockEntity> blockEntities;

    public BlockEntityIterator() {
        chunks = new ChunkIterator(false);

        nextChunk();
    }

    private void nextChunk() {
        while (true) {
            if (!chunks.hasNext()) break;

            Map<BlockPos, BlockEntity> blockEntityMap = ((ChunkAccessAccessor) chunks.next()).getBlockEntities();

            if (!blockEntityMap.isEmpty()) {
                blockEntities = blockEntityMap.values().iterator();
                break;
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (blockEntities == null) return false;
        if (blockEntities.hasNext()) return true;

        nextChunk();

        return blockEntities.hasNext();
    }

    @Override
    public BlockEntity next() {
        return blockEntities.next();
    }
}
