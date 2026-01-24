/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.blockesp;

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

public class ESPChunk {

    private final int x, z;
    public Long2ObjectMap<ESPBlock> blocks;

    public ESPChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public ESPBlock get(int x, int y, int z) {
        return blocks == null ? null : blocks.get(ESPBlock.getKey(x, y, z));
    }

    public void add(BlockPos blockPos, boolean update) {
        ESPBlock block = new ESPBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if (blocks == null) blocks = new Long2ObjectOpenHashMap<>(64);
        blocks.put(ESPBlock.getKey(blockPos), block);

        if (update) block.update();
    }

    public void add(BlockPos blockPos) {
        add(blockPos, true);
    }

    public void remove(BlockPos blockPos) {
        if (blocks != null) {
            ESPBlock block = blocks.remove(ESPBlock.getKey(blockPos));
            if (block != null) block.group.remove(block);
        }
    }

    public void update() {
        if (blocks != null) {
            for (ESPBlock block : blocks.values()) block.update();
        }
    }

    public void update(int x, int y, int z) {
        if (blocks != null) {
            ESPBlock block = blocks.get(ESPBlock.getKey(x, y, z));
            if (block != null) block.update();
        }
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
        if (blocks != null) {
            for (ESPBlock block : blocks.values()) block.render(event);
        }
    }


    public static ESPChunk searchChunk(Chunk chunk, List<Block> blocks1, List<Block> blocks2) {
        ESPChunk schunk = new ESPChunk(chunk.getPos().x, chunk.getPos().z);
        if (schunk.shouldBeDeleted()) return schunk;

        BlockPos.Mutable blockPos = new BlockPos.Mutable();

        // 优化：预检查列表，避免重复contains调用
        boolean hasBlocks1 = blocks1 != null && !blocks1.isEmpty();
        boolean hasBlocks2 = blocks2 != null && !blocks2.isEmpty();
        
        if (!hasBlocks1 && !hasBlocks2) return schunk; // 没有要搜索的方块

        for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++) {
            for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++) {
                int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - chunk.getPos().getStartX(), z - chunk.getPos().getStartZ());

                // 优化：从底部Y开始，但使用更智能的范围
                int startY = Math.max(mc.world.getBottomY(), height - 64); // 只搜索顶部64层
                for (int y = startY; y < height; y++) {
                    blockPos.set(x, y, z);
                    BlockState bs = chunk.getBlockState(blockPos);
                    Block block = bs.getBlock();

                    // 优化：快速检查，避免重复contains调用
                    boolean inGroup1 = hasBlocks1 && blocks1.contains(block);
                    boolean inGroup2 = hasBlocks2 && blocks2.contains(block);
                    
                    if (inGroup1 || inGroup2) {
                        schunk.add(blockPos, false);
                        
                        // 优化：如果已经找到很多方块，可能需要限制搜索
                        if (schunk.size() > 1000) {
                            return schunk; // 防止过度搜索
                        }
                    }
                }
            }
        }

        return schunk;
    }
}
