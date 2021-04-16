/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render.search;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.BlockUpdateEvent;
import minegame159.meteorclient.events.world.ChunkDataEvent;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.UnorderedArrayList;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.render.color.RainbowColors;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

import static minegame159.meteorclient.utils.Utils.getRenderDistance;

public class Search extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to search for.")
            .defaultValue(new ArrayList<>(0))
            .onChanged(blocks1 -> {
                if (isActive() && Utils.canUpdate()) onActivate();
            })
            .build()
    );

    private final Setting<SBlockData> defaultBlockConfig = sgGeneral.add(new GenericSetting.Builder<SBlockData>()
            .name("default-block-config")
            .description("Default block config.")
            .defaultValue(new SBlockData(ShapeMode.Lines, new SettingColor(0, 255, 200), new SettingColor(0, 255, 200, 25), true, new SettingColor(0, 255, 200, 125)))
            .build()
    );

    private final Setting<Map<Block, SBlockData>> blockConfigs = sgGeneral.add(new BlockDataSetting.Builder<SBlockData>()
            .name("block-configs")
            .description("Config for each block.")
            .defaultValue(new HashMap<>(0))
            .defaultData(defaultBlockConfig)
            .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
            .name("tracers")
            .description("Render tracer lines.")
            .defaultValue(false)
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private final Long2ObjectMap<SChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final List<SGroup> groups = new UnorderedArrayList<>();

    public Search() {
        super(Categories.Render, "search", "Searches for specified blocks.");

        RainbowColors.register(this::onTickRainbow);
    }

    @Override
    public void onActivate() {
        synchronized (chunks) {
            chunks.clear();
            groups.clear();
        }

        int viewDist = getRenderDistance() + 1;

        for (int x = mc.player.chunkX - viewDist; x <= mc.player.chunkX + viewDist; x++) {
            for (int z = mc.player.chunkZ - viewDist; z <= mc.player.chunkZ + viewDist; z++) {
                if (mc.world.getChunkManager().isChunkLoaded(x, z)) searchChunk(mc.world.getChunk(x, z), null);
            }
        }
    }

    @Override
    public void onDeactivate() {
        synchronized (chunks) {
            chunks.clear();
            groups.clear();
        }
    }

    private void onTickRainbow() {
        if (!isActive()) return;

        defaultBlockConfig.get().tickRainbow();
        for (SBlockData blockData : blockConfigs.get().values()) blockData.tickRainbow();
    }

    SBlockData getBlockData(Block block) {
        SBlockData blockData = blockConfigs.get().get(block);
        return blockData == null ? defaultBlockConfig.get() : blockData;
    }

    private void updateChunk(int x, int z) {
        SChunk chunk = chunks.get(ChunkPos.toLong(x, z));
        if (chunk != null) chunk.update();
    }

    private void updateBlock(int x, int y, int z) {
        SChunk chunk = chunks.get(ChunkPos.toLong(x >> 4, z >> 4));
        if (chunk != null) chunk.update(x, y, z);
    }

    public SBlock getBlock(int x, int y, int z) {
        SChunk chunk = chunks.get(ChunkPos.toLong(x >> 4, z >> 4));
        return chunk == null ? null : chunk.get(x, y, z);
    }

    public SGroup newGroup(Block block) {
        synchronized (chunks) {
            SGroup group = new SGroup(block);
            groups.add(group);
            return group;
        }
    }

    public void removeGroup(SGroup group) {
        synchronized (chunks) {
            groups.remove(group);
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        searchChunk(event.chunk, event);
    }

    private void searchChunk(Chunk chunk, ChunkDataEvent event) {
        MeteorExecutor.execute(() -> {
            if (!isActive()) return;
            SChunk schunk = SChunk.searchChunk(chunk, blocks.get());

            if (schunk.size() > 0) {
                synchronized (chunks) {
                    chunks.put(chunk.getPos().toLong(), schunk);
                    schunk.update();

                    // Update neighbour chunks
                    updateChunk(chunk.getPos().x - 1, chunk.getPos().z);
                    updateChunk(chunk.getPos().x + 1, chunk.getPos().z);
                    updateChunk(chunk.getPos().x, chunk.getPos().z - 1);
                    updateChunk(chunk.getPos().x, chunk.getPos().z + 1);
                }
            }

            if (event != null) ChunkDataEvent.returnChunkDataEvent(event);
        });
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        // Minecraft probably reuses the event.pos BlockPos instance because it causes problems when trying to use it inside another thread
        int bx = event.pos.getX();
        int by = event.pos.getY();
        int bz = event.pos.getZ();

        int chunkX = bx >> 4;
        int chunkZ = bz >> 4;
        long key = ChunkPos.toLong(chunkX, chunkZ);

        boolean added = blocks.get().contains(event.newState.getBlock()) && !blocks.get().contains(event.oldState.getBlock());
        boolean removed = !added && !blocks.get().contains(event.newState.getBlock()) && blocks.get().contains(event.oldState.getBlock());

        if (added || removed) {
            MeteorExecutor.execute(() -> {
                synchronized (chunks) {
                    SChunk chunk = chunks.get(key);

                    if (chunk == null) {
                        chunk = new SChunk(chunkX, chunkZ);
                        if (chunk.shouldBeDeleted()) return;

                        chunks.put(key, chunk);
                    }

                    blockPos.set(bx, by, bz);

                    if (added) chunk.add(blockPos);
                    else chunk.remove(blockPos);

                    // Update neighbour blocks
                    for (int x = -1; x < 2; x++) {
                        for (int z = -1; z < 2; z++) {
                            for (int y = -1; y < 2; y++) {
                                if (x == 0 && y == 0 && z == 0) continue;

                                updateBlock(bx + x, by + y, bz + z);
                            }
                        }
                    }
                }
            });
        }
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        synchronized (chunks) {
            for (Iterator<SChunk> it = chunks.values().iterator(); it.hasNext();) {
                SChunk chunk = it.next();

                if (chunk.shouldBeDeleted()) {
                    MeteorExecutor.execute(() -> {
                        for (SBlock block : chunk.blocks.values()) {
                            block.group.remove(block, false);
                            block.loaded = false;
                        }
                    });

                    it.remove();
                }
                else chunk.render();
            }

            if (tracers.get()) {
                for (Iterator<SGroup> it = groups.iterator(); it.hasNext();) {
                    SGroup group = it.next();

                    if (group.blocks.isEmpty()) it.remove();
                    else group.render(event);
                }
            }
        }
    }
}
