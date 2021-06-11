/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.world;

import minegame159.meteorclient.mixin.WorldChunkAccessor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Iterator;
import java.util.Map;

public class BlockEntityIterator implements Iterator<BlockEntity> {
    private final Iterator<WorldChunk> chunks;
    private Iterator<BlockEntity> blockEntities;

    public BlockEntityIterator() {
        chunks = new WorldChunkIterator();

        nextChunk();
    }

    private void nextChunk() {
        while (true) {
            if (!chunks.hasNext()) break;

            Map<BlockPos, BlockEntity> blockEntityMap = ((WorldChunkAccessor) chunks.next()).getBlockEntities();

            if (blockEntityMap.size() > 0) {
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
