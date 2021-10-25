/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.search;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getRenderDistance;

public class SChunk {
    private static final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private final int x, z;
    public Long2ObjectMap<SBlock> blocks;

    public SChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public SBlock get(int x, int y, int z) {
        return blocks == null ? null : blocks.get(SBlock.getKey(x, y, z));
    }

    public void add(BlockPos blockPos, boolean update) {
        SBlock block = new SBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if (blocks == null) blocks = new Long2ObjectOpenHashMap<>(64);
        blocks.put(SBlock.getKey(blockPos), block);

        if (update) block.update();
    }

    public void add(BlockPos blockPos) {
        add(blockPos, true);
    }

    public void remove(BlockPos blockPos) {
        SBlock block = blocks.remove(SBlock.getKey(blockPos));
        if (block != null) block.group.remove(block);
    }

    public void update() {
        for (SBlock block : blocks.values()) block.update();
    }

    public void update(int x, int y, int z) {
        SBlock block = blocks.get(SBlock.getKey(x, y, z));
        if (block != null) block.update();
    }

    public int size() {
        return blocks == null ? 0 : blocks.size();
    }

    public boolean shouldBeDeleted() {
        int viewDist = getRenderDistance() + 1;
        int chunkX = ChunkSectionPos.getSectionCoord(mc.player.getBlockPos().getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(mc.player.getBlockPos().getZ());

        return x > chunkX + viewDist || x < chunkX - viewDist || z > chunkZ + viewDist || z < chunkZ - viewDist;
    }

    public void render(Render3DEvent event) {
        for (SBlock block : blocks.values()) block.render(event);
    }


    public static SChunk searchChunk(Chunk chunk, List<Block> blocks) {
        SChunk schunk = new SChunk(chunk.getPos().x, chunk.getPos().z);
        if (schunk.shouldBeDeleted()) return schunk;

        for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++) {
            for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++) {
                int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - chunk.getPos().getStartX(), z - chunk.getPos().getStartZ());

                for (int y = mc.world.getBottomY(); y < height; y++) {
                    blockPos.set(x, y, z);
                    BlockState bs = chunk.getBlockState(blockPos);

                    if (blocks.contains(bs.getBlock())) schunk.add(blockPos, false);
                }
            }
        }

        return schunk;
    }
}
