/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.ChunkDataEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.network.MeteorExecutor;
import minegame159.meteorclient.utils.render.RenderUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public class Search extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgTracers = settings.createGroup("Tracers");

    private final Long2ObjectArrayMap<MyChunk> chunks = new Long2ObjectArrayMap<>();

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Blocks to search for.")
            .defaultValue(new ArrayList<>(0))
            .onChanged(blocks1 -> {
                if (Utils.canUpdate() && isActive()) {
                    synchronized (chunks) {
                        for (MyChunk chunk : chunks.values()) chunk.dispose();
                        chunks.clear();
                    }

                    searchViewDistance();
                }
            })
            .build()
    );

    // Render

    private final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
            .name("color")
            .description("The color.")
            .defaultValue(new SettingColor(0, 255, 200))
            .build()
    );

    private final Setting<Boolean> fullBlock = sgRender.add(new BoolSetting.Builder()
            .name("full-block")
            .description("If outlines will be rendered as full blocks.")
            .defaultValue(false)
            .build()
    );

    // Tracers

    private final Setting<Boolean> tracersEnabled = sgTracers.add(new BoolSetting.Builder()
            .name("tracers-enabled")
            .description("Draws lines to the blocks.")
            .defaultValue(false)
            .build()
    );

    private final Setting<SettingColor> tracersColor = sgTracers.add(new ColorSetting.Builder()
            .name("tracers-color")
            .description("The color of the tracers.")
            .defaultValue(new SettingColor(225, 225, 225))
            .build()
    );

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);

    private final LongList toRemove = new LongArrayList();
    private final LongList toUpdate = new LongArrayList();

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private DimensionType lastDimension;

    public Search() {
        super(Categories.Render, "search", "Searches for specified blocks.");
    }

    @Override
    public void onActivate() {
        lastDimension = mc.world.getDimension();

        searchViewDistance();
    }

    @Override
    public void onDeactivate() {
        for (MyChunk chunk : chunks.values()) chunk.dispose();
        chunks.clear();
    }

    private void searchViewDistance() {
        int viewDist = mc.options.viewDistance;
        for (int x = mc.player.chunkX - viewDist; x <= mc.player.chunkX + viewDist; x++) {
            for (int z = mc.player.chunkZ - viewDist; z <= mc.player.chunkZ + viewDist; z++) {
                if (mc.world.getChunkManager().isChunkLoaded(x, z)) searchChunk(mc.world.getChunk(x, z), null);
            }
        }
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        searchChunk(event.chunk, event);
    }

    private void searchChunk(Chunk chunk, ChunkDataEvent event) {
        MeteorExecutor.execute(() -> {
            MyChunk myChunk = new MyChunk(chunk.getPos().x, chunk.getPos().z);

            for (int x = chunk.getPos().getStartX(); x <= chunk.getPos().getEndX(); x++) {
                for (int z = chunk.getPos().getStartZ(); z <= chunk.getPos().getEndZ(); z++) {
                    int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - chunk.getPos().getStartX(), z - chunk.getPos().getStartZ());
                    for (int y = 0; y < height; y++) {
                        blockPos.set(x, y, z);
                        BlockState bs = chunk.getBlockState(blockPos);
                        if (blocks.get().contains(bs.getBlock())) myChunk.add(blockPos, false);
                    }
                }
            }

            synchronized (chunks) {
                if (myChunk.blocks.size() > 0) chunks.put(chunk.getPos().toLong(), myChunk);
            }

            if (event != null) ChunkDataEvent.returnChunkDataEvent(event);
        });
    }

    public void onBlockUpdate(BlockPos blockPos, BlockState blockState) {
        MeteorExecutor.execute(() -> {
            int chunkX = blockPos.getX() >> 4;
            int chunkZ = blockPos.getZ() >> 4;
            long key = ChunkPos.toLong(chunkX, chunkZ);

            synchronized (chunks) {
                if (blocks.get().contains(blockState.getBlock())) {
                    chunks.computeIfAbsent(key, aLong -> new MyChunk(chunkX, chunkZ)).add(blockPos, true);
                } else {
                    MyChunk chunk = chunks.get(key);
                    if (chunk != null) chunk.remove(blockPos);
                }
            }
        });
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (lastDimension != mc.world.getDimension()) {
            synchronized (chunks) {
                for (MyChunk chunk : chunks.values()) chunk.dispose();
                chunks.clear();
            }
        }

        synchronized (chunks) {
            for (long key : toUpdate) {
                MyChunk chunk = chunks.get(key);
                if (chunk != null) chunk.update();
            }
            toUpdate.clear();
        }

        lastDimension = mc.world.getDimension();
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        synchronized (chunks) {
            toRemove.clear();
            
            for (long key : chunks.keySet()) {
                MyChunk chunk = chunks.get(key);
                if (chunk.shouldBeDeleted()) toRemove.add(key);
                else chunk.render(event);
            }
            
            for (long key : toRemove) {
                chunks.remove(key);
            }
        }
    }

    private void addToUpdate(int x, int z) {
        long key = ChunkPos.toLong(x, z);
        if (chunks.containsKey(key) && !toUpdate.contains(key)) toUpdate.add(key);
    }

    private class MyChunk {
        private final int x, z;
        private final List<MyBlock> blocks = new ArrayList<>();

        public MyChunk(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public void add(BlockPos blockPos, boolean checkForDuplicates) {
            if (checkForDuplicates) {
                for (MyBlock block : blocks) {
                    if (block.equals(blockPos)) return;
                }
            }

            MyBlock block = blockPool.get();
            block.set(blockPos);
            blocks.add(block);

            addToUpdateChunk(blockPos);
        }

        public void remove(BlockPos blockPos) {
            for (int i = 0; i < blocks.size(); i++) {
                MyBlock block = blocks.get(i);

                if (block.equals(blockPos)) {
                    blocks.remove(i);
                    return;
                }
            }

            addToUpdateChunk(blockPos);
        }

        private void addToUpdateChunk(BlockPos blockPos) {
            addToUpdate(x, z);

            double aX = Math.abs(blockPos.getX() + (blockPos.getX() < 0 ? 1 : 0)) % 16;
            double aZ = Math.abs(blockPos.getZ() + (blockPos.getZ() < 0 ? 1 : 0)) % 16;

            if (aX == 15) addToUpdate(x + (blockPos.getX() < 0 ? -1 : 1), z);
            else if (aX == 0) addToUpdate(x - (blockPos.getX() < 0 ? -1 : 1), z);
            if (aZ == 15) addToUpdate(x, z + (blockPos.getZ() < 0 ? -1 : 1));
            else if (aZ == 0) addToUpdate(x, z - (blockPos.getZ() < 0 ? -1 : 1));
        }

        public boolean shouldBeDeleted() {
            int viewDist = mc.options.viewDistance + 1;
            return x > mc.player.chunkX + viewDist || x < mc.player.chunkX - viewDist || z > mc.player.chunkZ + viewDist || z < mc.player.chunkZ - viewDist;
        }

        public void update() {
            for (MyBlock block : blocks) block.updateNeighbours();
        }

        public void render(RenderEvent event) {
            for (MyBlock block : blocks) block.render(event);
        }

        public void dispose() {
            for (MyBlock block : blocks) blockPool.free(block);
            blocks.clear();
        }
    }

    private static final BlockPos.Mutable BLOCK_POS = new BlockPos.Mutable();

    private class MyBlock {
        private static final int FO = 1 << 1;
        private static final int FO_RI = 1 << 2;
        private static final int RI = 1 << 3;
        private static final int BA_RI = 1 << 4;
        private static final int BA = 1 << 5;
        private static final int BA_LE = 1 << 6;
        private static final int LE = 1 << 7;
        private static final int FO_LE = 1 << 8;

        private static final int TO = 1 << 9;
        private static final int TO_FO = 1 << 10;
        private static final int TO_BA = 1 << 11;
        private static final int TO_RI = 1 << 12;
        private static final int TO_LE = 1 << 13;
        private static final int BO = 1 << 14;
        private static final int BO_FO = 1 << 15;
        private static final int BO_BA = 1 << 16;
        private static final int BO_RI = 1 << 17;
        private static final int BO_LE = 1 << 18;

        private int x, y, z;
        private BlockState state;
        private int neighbours;

        public void set(BlockPos blockPos) {
            x = blockPos.getX();
            y = blockPos.getY();
            z = blockPos.getZ();

            state = mc.world.getBlockState(blockPos);

            updateNeighbours();
        }

        public void updateNeighbours() {
            neighbours = 0;

            if (isBlock(0, 0, 1)) neighbours |= FO;
            if (isBlock(1, 0, 1)) neighbours |= FO_RI;
            if (isBlock(1, 0, 0)) neighbours |= RI;
            if (isBlock(1, 0, -1)) neighbours |= BA_RI;
            if (isBlock(0, 0, -1)) neighbours |= BA;
            if (isBlock(-1, 0, -1)) neighbours |= BA_LE;
            if (isBlock(-1, 0, 0)) neighbours |= LE;
            if (isBlock(-1, 0, 1)) neighbours |= FO_LE;

            if (isBlock(0, 1, 0)) neighbours |= TO;
            if (isBlock(0, 1, 1)) neighbours |= TO_FO;
            if (isBlock(0, 1, -1)) neighbours |= TO_BA;
            if (isBlock(1, 1, 0)) neighbours |= TO_RI;
            if (isBlock(-1, 1, 0)) neighbours |= TO_LE;
            if (isBlock(0, -1, 0)) neighbours |= BO;
            if (isBlock(0, -1, 1)) neighbours |= BO_FO;
            if (isBlock(0, -1, -1)) neighbours |= BO_BA;
            if (isBlock(1, -1, 0)) neighbours |= BO_RI;
            if (isBlock(-1, -1, 0)) neighbours |= BO_LE;
        }

        private boolean isBlock(double x, double y, double z) {
            BLOCK_POS.set(this.x + x, this.y + y, this.z + z);
            return mc.world.getBlockState(BLOCK_POS).getBlock() == state.getBlock();
        }

        public void render(RenderEvent event) {
            double x1 = x;
            double y1 = y;
            double z1 = z;
            double x2 = x + 1;
            double y2 = y + 1;
            double z2 = z + 1;

            boolean fullCube = true;

            if (!fullBlock.get()) {
                VoxelShape shape = state.getOutlineShape(mc.world, blockPos);
                fullCube = Block.isShapeFullCube(shape);

                if (!shape.isEmpty()) {
                    x1 = x + shape.getMin(Direction.Axis.X);
                    y1 = y + shape.getMin(Direction.Axis.Y);
                    z1 = z + shape.getMin(Direction.Axis.Z);
                    x2 = x + shape.getMax(Direction.Axis.X);
                    y2 = y + shape.getMax(Direction.Axis.Y);
                    z2 = z + shape.getMax(Direction.Axis.Z);
                }
            }

            if (fullCube) {
                // Vertical, BA_LE
                if (((neighbours & LE) != LE && (neighbours & BA) != BA) || ((neighbours & LE) == LE && (neighbours & BA) == BA && (neighbours & BA_LE) != BA_LE)) {
                    Renderer.LINES.line(x1, y1, z1, x1, y2, z1, color.get());
                }
                // Vertical, FO_LE
                if (((neighbours & LE) != LE && (neighbours & FO) != FO) || ((neighbours & LE) == LE && (neighbours & FO) == FO && (neighbours & FO_LE) != FO_LE)) {
                    Renderer.LINES.line(x1, y1, z2, x1, y2, z2, color.get());
                }
                // Vertical, BA_RI
                if (((neighbours & RI) != RI && (neighbours & BA) != BA) || ((neighbours & RI) == RI && (neighbours & BA) == BA && (neighbours & BA_RI) != BA_RI)) {
                    Renderer.LINES.line(x2, y1, z1, x2, y2, z1, color.get());
                }
                // Vertical, FO_RI
                if (((neighbours & RI) != RI && (neighbours & FO) != FO) || ((neighbours & RI) == RI && (neighbours & FO) == FO && (neighbours & FO_RI) != FO_RI)) {
                    Renderer.LINES.line(x2, y1, z2, x2, y2, z2, color.get());
                }

                // Horizontal bottom, BA_LE - BA_RI
                if (((neighbours & BA) != BA && (neighbours & BO) != BO) || ((neighbours & BA) != BA && (neighbours & BO_BA) == BO_BA)) {
                    Renderer.LINES.line(x1, y1, z1, x2, y1, z1, color.get());
                }
                // Horizontal bottom, FO_LE - FO_RI
                if (((neighbours & FO) != FO && (neighbours & BO) != BO) || ((neighbours & FO) != FO && (neighbours & BO_FO) == BO_FO)) {
                    Renderer.LINES.line(x1, y1, z2, x2, y1, z2, color.get());
                }
                // Horizontal top, BA_LE - BA_RI
                if (((neighbours & BA) != BA && (neighbours & TO) != TO) || ((neighbours & BA) != BA && (neighbours & TO_BA) == TO_BA)) {
                    Renderer.LINES.line(x1, y2, z1, x2, y2, z1, color.get());
                }
                // Horizontal top, FO_LE - FO_RI
                if (((neighbours & FO) != FO && (neighbours & TO) != TO) || ((neighbours & FO) != FO && (neighbours & TO_FO) == TO_FO)) {
                    Renderer.LINES.line(x1, y2, z2, x2, y2, z2, color.get());
                }

                // Horizontal bottom, BA_LE - FO_LE
                if (((neighbours & LE) != LE && (neighbours & BO) != BO) || ((neighbours & LE) != LE && (neighbours & BO_LE) == BO_LE)) {
                    Renderer.LINES.line(x1, y1, z1, x1, y1, z2, color.get());
                }
                // Horizontal bottom, BA_RI - FO_RI
                if (((neighbours & RI) != RI && (neighbours & BO) != BO) || ((neighbours & RI) != RI && (neighbours & BO_RI) == BO_RI)) {
                    Renderer.LINES.line(x2, y1, z1, x2, y1, z2, color.get());
                }
                // Horizontal top, BA_LE - FO_LE
                if (((neighbours & LE) != LE && (neighbours & TO) != TO) || ((neighbours & LE) != LE && (neighbours & TO_LE) == TO_LE)) {
                    Renderer.LINES.line(x1, y2, z1, x1, y2, z2, color.get());
                }
                // Horizontal top, BA_RI - FO_RI
                if (((neighbours & RI) != RI && (neighbours & TO) != TO) || ((neighbours & RI) != RI && (neighbours & TO_RI) == TO_RI)) {
                    Renderer.LINES.line(x2, y2, z1, x2, y2, z2, color.get());
                }
            } else {
                Renderer.LINES.boxEdges(x1, y1, z1, x2, y2, z2, color.get(), 0);
            }

            // Tracers
            if (tracersEnabled.get()) RenderUtils.drawTracerToPos(new BlockPos(x, y, z), tracersColor.get(), event);
        }

        public boolean equals(BlockPos blockPos) {
            return x == blockPos.getX() && y == blockPos.getY() && z == blockPos.getZ();
        }
    }
}
