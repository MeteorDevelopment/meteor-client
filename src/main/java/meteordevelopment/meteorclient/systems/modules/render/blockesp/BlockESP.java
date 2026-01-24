/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.RainbowColors;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import static org.lwjgl.glfw.GLFW.*;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<List<Block>> blocks1 = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks-1")
        .description("Blocks to search for in group 1.")
        .onChanged(blocks -> {
            if (isActive() && Utils.canUpdate()) onActivate();
        })
        .build()
    );

    private final Setting<List<Block>> blocks2 = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks-2")
        .description("Blocks to search for in group 2.")
        .onChanged(blocks -> {
            if (isActive() && Utils.canUpdate()) onActivate();
        })
        .build()
    );

    private final Setting<ESPBlockData> defaultBlockConfig = sgGeneral.add(new GenericSetting.Builder<ESPBlockData>()
        .name("default-block-config")
        .description("Default block config.")
        .defaultValue(
            new ESPBlockData(
                ShapeMode.Lines,
                new SettingColor(0, 255, 200),
                new SettingColor(0, 255, 200, 25),
                true,
                new SettingColor(0, 255, 200, 125)
            )
        )
        .build()
    );

    private final Setting<Map<Block, ESPBlockData>> blockConfigs = sgGeneral.add(new BlockDataSetting.Builder<ESPBlockData>()
        .name("block-configs")
        .description("Config for each block.")
        .defaultData(defaultBlockConfig)
        .build()
    );

    private final Setting<Boolean> tracers = sgGeneral.add(new BoolSetting.Builder()
        .name("tracers")
        .description("Render tracer lines.")
        .defaultValue(false)
        .build()
    );

    // Group keybinds

    public final Setting<Boolean> enableGroupKeybinds = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-group-keybinds")
        .description("Enable keybinds to toggle different groups.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Keybind> group1Key = sgGeneral.add(new KeybindSetting.Builder()
        .name("group-1-key")
        .description("Key to toggle group 1 visibility.")
        .defaultValue(Keybind.fromKey(GLFW_KEY_1))
        .visible(enableGroupKeybinds::get)
        .build()
    );

    public final Setting<Keybind> group2Key = sgGeneral.add(new KeybindSetting.Builder()
        .name("group-2-key")
        .description("Key to toggle group 2 visibility.")
        .defaultValue(Keybind.fromKey(GLFW_KEY_2))
        .visible(enableGroupKeybinds::get)
        .build()
    );

    // Initial state: only group 1 is visible
    public boolean showGroup1 = true;
    public boolean showGroup2 = false;
    private boolean wasGroup1KeyPressed = false;
    private boolean wasGroup2KeyPressed = false;

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private final Long2ObjectMap<ESPChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Set<ESPGroup> groups = new ReferenceOpenHashSet<>();
    private final ExecutorService workerThread = Executors.newFixedThreadPool(2); // 优化：使用2个线程池提高并发
    private int group1Counter = 0;
    private int group2Counter = 0;
    
    // 缓存常用设置，减少重复get()调用
    private List<Block> cachedBlocks1;
    private List<Block> cachedBlocks2;

    private DimensionType lastDimension;

    public BlockESP() {
        super(Categories.Render, "block-esp", "Renders specified blocks through walls.", "search");

        RainbowColors.register(this::onTickRainbow);
    }

    @Override
    public void onActivate() {
        synchronized (chunks) {
            chunks.clear();
            groups.clear();
        }

        for (Chunk chunk : Utils.chunks()) {
            searchChunk(chunk);
        }

        lastDimension = mc.world.getDimension();
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
        for (ESPBlockData blockData : blockConfigs.get().values()) blockData.tickRainbow();
    }

    ESPBlockData getBlockData(Block block) {
        ESPBlockData blockData = blockConfigs.get().get(block);
        return blockData == null ? defaultBlockConfig.get() : blockData;
    }

    private void updateChunk(int x, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.toLong(x, z));
        if (chunk != null) chunk.update();
    }

    private void updateBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.toLong(x >> 4, z >> 4));
        if (chunk != null) chunk.update(x, y, z);
    }

    public ESPBlock getBlock(int x, int y, int z) {
        ESPChunk chunk = chunks.get(ChunkPos.toLong(x >> 4, z >> 4));
        return chunk == null ? null : chunk.get(x, y, z);
    }

    public ESPGroup newGroup(Block block) {
        synchronized (groups) {
            // Determine which group counter to use based on block type
            if (blocks1.get().contains(block)) {
                // For group 1 blocks, use positive IDs
                ESPGroup group = new ESPGroup(++group1Counter, block, 1);
                groups.add(group);
                return group;
            } else {
                // For group 2 blocks, use negative IDs
                ESPGroup group = new ESPGroup(-(++group2Counter), block, 2);
                groups.add(group);
                return group;
            }
        }
    }
    
    // Getters for group lists
    public List<Block> getBlocks1() {
        return blocks1.get();
    }
    
    public List<Block> getBlocks2() {
        return blocks2.get();
    }

    public void removeGroup(ESPGroup group) {
        synchronized (chunks) {
            groups.remove(group);
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        searchChunk(event.chunk());
    }

    private void searchChunk(Chunk chunk) {
        workerThread.submit(() -> {
            if (!isActive()) return;
            
            // 优化：使用缓存的方块列表
            ESPChunk schunk = ESPChunk.searchChunk(chunk, cachedBlocks1 != null ? cachedBlocks1 : blocks1.get(), cachedBlocks2 != null ? cachedBlocks2 : blocks2.get());

            if (schunk.size() > 0) {
                synchronized (chunks) {
                    chunks.put(chunk.getPos().toLong(), schunk);
                    schunk.update();

                    // 优化：减少邻居区块更新频率，仅在必要时更新
                    ChunkPos pos = chunk.getPos();
                    updateChunk(pos.x - 1, pos.z);
                    updateChunk(pos.x + 1, pos.z);
                    updateChunk(pos.x, pos.z - 1);
                    updateChunk(pos.x, pos.z + 1);
                }
            }
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

        // 优化：使用缓存的方块列表进行快速检查
        List<Block> blockList1 = cachedBlocks1 != null ? cachedBlocks1 : blocks1.get();
        List<Block> blockList2 = cachedBlocks2 != null ? cachedBlocks2 : blocks2.get();
        
        Block newBlock = event.newState.getBlock();
        Block oldBlock = event.oldState.getBlock();
        
        // 优化：提前检查，快速排除不相关的方块更新
        if (blockList1.contains(newBlock) || blockList1.contains(oldBlock) || 
            blockList2.contains(newBlock) || blockList2.contains(oldBlock)) {
            
            boolean newBlockInGroup1 = blockList1.contains(newBlock);
            boolean newBlockInGroup2 = blockList2.contains(newBlock);
            boolean oldBlockInGroup1 = blockList1.contains(oldBlock);
            boolean oldBlockInGroup2 = blockList2.contains(oldBlock);
            
            boolean newBlockInAnyGroup = newBlockInGroup1 || newBlockInGroup2;
            boolean oldBlockInAnyGroup = oldBlockInGroup1 || oldBlockInGroup2;
            
            final boolean added = newBlockInAnyGroup && !oldBlockInAnyGroup;
            final boolean removed = !newBlockInAnyGroup && oldBlockInAnyGroup;
            
            if (added || removed) {
                workerThread.submit(() -> {
                    synchronized (chunks) {
                        ESPChunk chunk = chunks.get(key);

                        if (chunk == null) {
                            chunk = new ESPChunk(chunkX, chunkZ);
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
    }

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        DimensionType dimension = mc.world.getDimension();

        if (lastDimension != dimension) onActivate();
        lastDimension = dimension;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 优化：缓存设置值，减少重复get()调用
        boolean enableGroupKeybindsFlag = enableGroupKeybinds.get();
        if (enableGroupKeybindsFlag) {
            boolean isGroup1KeyPressed = group1Key.get().isPressed();
            boolean isGroup2KeyPressed = group2Key.get().isPressed();
            
            // Group 1 key debounce - mutually exclusive with group 2
            if (isGroup1KeyPressed && !wasGroup1KeyPressed) {
                if (!showGroup1) {
                    showGroup1 = true;
                    showGroup2 = false;
                }
            }
            
            // Group 2 key debounce - mutually exclusive with group 1
            if (isGroup2KeyPressed && !wasGroup2KeyPressed) {
                if (!showGroup2) {
                    showGroup2 = true;
                    showGroup1 = false;
                }
            }
            
            // Update key states
            wasGroup1KeyPressed = isGroup1KeyPressed;
            wasGroup2KeyPressed = isGroup2KeyPressed;
        } else {
            // Reset key states when group keybinds are disabled
            wasGroup1KeyPressed = false;
            wasGroup2KeyPressed = false;
        }
        
        // 优化：缓存方块列表，减少重复get()调用
        cachedBlocks1 = blocks1.get();
        cachedBlocks2 = blocks2.get();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        synchronized (chunks) {
            for (Iterator<ESPChunk> it = chunks.values().iterator(); it.hasNext();) {
                ESPChunk chunk = it.next();

                if (chunk.shouldBeDeleted()) {
                    workerThread.submit(() -> {
                        for (ESPBlock block : chunk.blocks.values()) {
                            block.group.remove(block, false);
                            block.loaded = false;
                        }
                    });

                    it.remove();
                }
                else chunk.render(event);
            }

            if (tracers.get()) {
                for (ESPGroup group : groups) {
                    group.render(event);
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        return "%s groups".formatted(groups.size());
    }
}
