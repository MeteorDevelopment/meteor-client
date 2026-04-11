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
import java.util.concurrent.TimeUnit;

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
        .description("Key to activate group 1 (mutually exclusive with group 2).")
        .defaultValue(Keybind.fromKey(GLFW_KEY_1))
        .visible(enableGroupKeybinds::get)
        .build()
    );

    public final Setting<Keybind> group2Key = sgGeneral.add(new KeybindSetting.Builder()
        .name("group-2-key")
        .description("Key to activate group 2 (mutually exclusive with group 1).")
        .defaultValue(Keybind.fromKey(GLFW_KEY_2))
        .visible(enableGroupKeybinds::get)
        .build()
    );

    // 初始状态：只显示 group 1
    public boolean showGroup1 = true;
    public boolean showGroup2 = false;
    private boolean wasGroup1KeyPressed = false;
    private boolean wasGroup2KeyPressed = false;

    // Fix #2: 不再使用 Mutable 成员变量跨线程传递，改为在 onBlockUpdate 里捕获局部 int
    private final Long2ObjectMap<ESPChunk> chunks = new Long2ObjectOpenHashMap<>();

    // Fix #6: 统一使用 chunks 作为锁对象，消除 chunks/groups 双锁不一致问题
    private final Set<ESPGroup> groups = new ReferenceOpenHashSet<>();

    private ExecutorService workerThread;
    private int group1Counter = 0;
    private int group2Counter = 0;

    private DimensionType lastDimension;

    public BlockESP() {
        super(Categories.Render, "block-esp", "Renders specified blocks through walls.", "search");

        RainbowColors.register(this::onTickRainbow);
    }

    @Override
    public void onActivate() {
        // Fix #5: 每次激活时创建新的线程池（防止上次 deactivate 后残留关闭状态）
        if (workerThread == null || workerThread.isShutdown()) {
            workerThread = Executors.newFixedThreadPool(2);
        }

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

        // Fix #5: 关闭线程池，释放资源，等待最多 2 秒让任务安全结束
        if (workerThread != null && !workerThread.isShutdown()) {
            workerThread.shutdown();
            try {
                if (!workerThread.awaitTermination(2, TimeUnit.SECONDS)) {
                    workerThread.shutdownNow();
                }
            } catch (InterruptedException e) {
                workerThread.shutdownNow();
                Thread.currentThread().interrupt();
            }
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

    // Fix #6: 统一用 chunks 锁
    public ESPGroup newGroup(Block block) {
        synchronized (chunks) {
            if (blocks1.get().contains(block)) {
                ESPGroup group = new ESPGroup(++group1Counter, block, 1);
                groups.add(group);
                return group;
            } else {
                ESPGroup group = new ESPGroup(-(++group2Counter), block, 2);
                groups.add(group);
                return group;
            }
        }
    }

    public List<Block> getBlocks1() {
        return blocks1.get();
    }

    public List<Block> getBlocks2() {
        return blocks2.get();
    }

    // Fix #6: 统一用 chunks 锁（原来用的是 chunks 但注释写的 groups，保持一致）
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
        // Fix #3: 直接调用 blocks1/2.get()，不依赖可能为 null 的缓存字段
        // 缓存的意义仅在同一 tick 内复用，不应跨方法传递
        List<Block> b1 = blocks1.get();
        List<Block> b2 = blocks2.get();

        workerThread.submit(() -> {
            if (!isActive()) return;

            ESPChunk schunk = ESPChunk.searchChunk(chunk, b1, b2);

            if (schunk.size() > 0) {
                synchronized (chunks) {
                    chunks.put(chunk.getPos().toLong(), schunk);
                    schunk.update();

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
        // Fix #2: 在主线程立刻捕获坐标为局部 int，避免 Mutable 成员变量跨线程竞态
        final int bx = event.pos.getX();
        final int by = event.pos.getY();
        final int bz = event.pos.getZ();

        final int chunkX = bx >> 4;
        final int chunkZ = bz >> 4;
        final long key   = ChunkPos.toLong(chunkX, chunkZ);

        List<Block> blockList1 = blocks1.get();
        List<Block> blockList2 = blocks2.get();

        Block newBlock = event.newState.getBlock();
        Block oldBlock = event.oldState.getBlock();

        boolean newInGroup1 = blockList1.contains(newBlock);
        boolean newInGroup2 = blockList2.contains(newBlock);
        boolean oldInGroup1 = blockList1.contains(oldBlock);
        boolean oldInGroup2 = blockList2.contains(oldBlock);

        boolean newInAny = newInGroup1 || newInGroup2;
        boolean oldInAny = oldInGroup1 || oldInGroup2;

        final boolean added   = newInAny && !oldInAny;
        final boolean removed = !newInAny && oldInAny;

        if (!added && !removed) return;

        workerThread.submit(() -> {
            synchronized (chunks) {
                ESPChunk chunk = chunks.get(key);

                if (chunk == null) {
                    chunk = new ESPChunk(chunkX, chunkZ);
                    if (chunk.shouldBeDeleted()) return;
                    chunks.put(key, chunk);
                }

                // Fix #2: 使用局部捕获的坐标创建新的 BlockPos，不复用 Mutable 成员变量
                BlockPos pos = new BlockPos(bx, by, bz);

                if (added)   chunk.add(pos);
                else         chunk.remove(pos);

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

    @EventHandler
    private void onPostTick(TickEvent.Post event) {
        DimensionType dimension = mc.world.getDimension();
        if (lastDimension != dimension) onActivate();
        lastDimension = dimension;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (enableGroupKeybinds.get()) {
            boolean isGroup1KeyPressed = group1Key.get().isPressed();
            boolean isGroup2KeyPressed = group2Key.get().isPressed();

            // 单向激活 + 互斥：按下时切换到该组（如果已在该组则无反应）
            if (isGroup1KeyPressed && !wasGroup1KeyPressed && !showGroup1) {
                showGroup1 = true;
                showGroup2 = false;
            }

            if (isGroup2KeyPressed && !wasGroup2KeyPressed && !showGroup2) {
                showGroup2 = true;
                showGroup1 = false;
            }

            wasGroup1KeyPressed = isGroup1KeyPressed;
            wasGroup2KeyPressed = isGroup2KeyPressed;
        } else {
            wasGroup1KeyPressed = false;
            wasGroup2KeyPressed = false;
        }
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
                } else {
                    chunk.render(event);
                }
            }

            if (tracers.get()) {
                for (ESPGroup group : groups) {
                    group.render(event);
                }
            }
        }
    }

    // Fix #7: getInfoString 在渲染线程调用，加锁保护 groups 读取
    @Override
    public String getInfoString() {
        synchronized (chunks) {
            return "%s groups".formatted(groups.size());
        }
    }
}