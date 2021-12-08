/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Iterator;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ChunkIterator implements Iterator<Chunk> {
    private final int px, pz;
    private final int r;

    private int x, z;
    private WorldChunk chunk;

    public ChunkIterator() {
        px = ChunkSectionPos.getSectionCoord(mc.player.getBlockX());
        pz = ChunkSectionPos.getSectionCoord(mc.player.getBlockZ());
        r = Utils.getRenderDistance();

        x = px - r;
        z = pz - r;

        nextChunk();
    }

    private void nextChunk() {
        chunk = null;

        while (true) {
            z++;
            if (z > pz + r) {
                z = pz - r;
                x++;
            }

            if (x > px + r || z > pz + r) break;

            chunk = (WorldChunk) mc.world.getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk != null) break;
        }
    }

    @Override
    public boolean hasNext() {
        return chunk != null;
    }

    @Override
    public WorldChunk next() {
        WorldChunk chunk = this.chunk;

        nextChunk();

        return chunk;
    }
}
