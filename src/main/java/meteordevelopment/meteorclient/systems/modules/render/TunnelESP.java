/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

public class TunnelESP extends Module {
    private static final BlockPos.Mutable BP = new BlockPos.Mutable();
    private static final Direction[] DIRECTIONS = { Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST };

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
        .name("height")
        .description("Height of the rendered box.")
        .defaultValue(0.1)
        .sliderMax(2)
        .build()
    );

    private final Setting<Boolean> connected = sgGeneral.add(new BoolSetting.Builder()
        .name("connected")
        .description("If neighbouring holes should be connected.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color.")
        .defaultValue(new SettingColor(255, 175, 25, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color.")
        .defaultValue(new SettingColor(255, 175, 25, 255))
        .build()
    );

    private final Long2ObjectMap<TChunk> chunks = new Long2ObjectOpenHashMap<>();

    public TunnelESP() {
        super(Categories.Render, "tunnel-ESP", "Highlights tunnels.");
    }

    @Override
    public void onDeactivate() {
        chunks.clear();
    }

    private static int pack(int x, int y, int z) {
        return ((x & 0xFF) << 24) | ((y & 0xFFFF) << 8) | (z & 0xFF);
    }

    private static byte getPackedX(int p) {
        return (byte) (p >> 24 & 0xFF);
    }

    private static short getPackedY(int p) {
        return (short) (p >> 8 & 0xFFFF);
    }

    private static byte getPackedZ(int p) {
        return (byte) (p & 0xFF);
    }

    private void searchChunk(Chunk chunk, TChunk tChunk) {
        // Prepare variables
        Context ctx = new Context();
        IntSet set = new IntOpenHashSet();

        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();

        int endX = chunk.getPos().getEndX();
        int endZ = chunk.getPos().getEndZ();

        // Search for first set of tunnels
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                int height = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - startX, z - startZ);

                for (short y = (short) mc.world.getBottomY(); y < height; y++) {
                    if (isTunnel(ctx, x, y, z)) set.add(pack(x - startX, y, z - startZ));
                }
            }
        }

        // Remove tunnels which are 1 block long
        IntSet positions = new IntOpenHashSet();

        for (IntIterator it = set.iterator(); it.hasNext();) {
            int packed = it.nextInt();

            byte x = getPackedX(packed);
            short y = getPackedY(packed);
            byte z = getPackedZ(packed);

            if (x == 0 || x == 15 || z == 0 || z == 15) positions.add(packed);
            else {
                boolean has = false;

                for (Direction dir : DIRECTIONS) {
                    if (set.contains(pack(x + dir.getOffsetX(), y, z + dir.getOffsetZ()))) {
                        has = true;
                        break;
                    }
                }

                if (has) positions.add(packed);
            }
        }

        tChunk.positions = positions;
    }

    private boolean isTunnel(Context ctx, int x, int y, int z) {
        if (!canWalkIn(ctx, x, y, z)) return false;

        TunnelSide s1 = getTunnelSide(ctx, x + 1, y, z);
        if (s1 == TunnelSide.PartiallyBlocked) return false;

        TunnelSide s2 = getTunnelSide(ctx, x - 1, y, z);
        if (s2 == TunnelSide.PartiallyBlocked) return false;

        TunnelSide s3 = getTunnelSide(ctx, x, y, z + 1);
        if (s3 == TunnelSide.PartiallyBlocked) return false;

        TunnelSide s4 = getTunnelSide(ctx, x, y, z - 1);
        if (s4 == TunnelSide.PartiallyBlocked) return false;

        return (s1 == TunnelSide.Walkable && s2 == TunnelSide.Walkable && s3 == TunnelSide.FullyBlocked && s4 == TunnelSide.FullyBlocked) || (s1 == TunnelSide.FullyBlocked && s2 == TunnelSide.FullyBlocked && s3 == TunnelSide.Walkable && s4 == TunnelSide.Walkable);
    }

    private TunnelSide getTunnelSide(Context ctx, int x, int y, int z) {
        if (canWalkIn(ctx, x, y, z)) return TunnelSide.Walkable;
        if (!canWalkThrough(ctx, x, y, z) && !canWalkThrough(ctx, x, y + 1, z)) return TunnelSide.FullyBlocked;
        return TunnelSide.PartiallyBlocked;
    }

    private boolean canWalkOn(Context ctx, int x, int y, int z) {
        BlockState state = ctx.get(x, y, z);

        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false;

        return !state.getCollisionShape(mc.world, BP.set(x, y, z)).isEmpty();
    }

    private boolean canWalkThrough(Context ctx, int x, int y, int z) {
        BlockState state = ctx.get(x, y, z);

        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return false;

        return state.getCollisionShape(mc.world, BP.set(x, y, z)).isEmpty();
    }

    private boolean canWalkIn(Context ctx, int x, int y, int z) {
        if (!canWalkOn(ctx, x, y - 1, z)) return false;
        if (!canWalkThrough(ctx, x, y, z)) return false;
        if (canWalkThrough(ctx, x, y + 2, z)) return false;
        return canWalkThrough(ctx, x, y + 1, z);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        synchronized (chunks) {
            for (TChunk tChunk : chunks.values()) tChunk.marked = false;
            int added = 0;

            for (Chunk chunk : Utils.chunks(true)) {
                long key = ChunkPos.toLong(chunk.getPos().x, chunk.getPos().z);

                if (chunks.containsKey(key)) chunks.get(key).marked = true;
                else if (added < 48) {
                    TChunk tChunk = new TChunk(chunk.getPos().x, chunk.getPos().z);
                    chunks.put(tChunk.getKey(), tChunk);

                    MeteorExecutor.execute(() -> searchChunk(chunk, tChunk));
                    added++;
                }
            }

            chunks.values().removeIf(tChunk -> !tChunk.marked);
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        synchronized (chunks) {
            for (TChunk chunk : chunks.values()) chunk.render(event.renderer);
        }
    }

    private boolean chunkContains(TChunk chunk, int x, int y, int z) {
        int key;

        if (x == -1) {
            chunk = chunks.get(ChunkPos.toLong(chunk.x - 1, chunk.z));
            key = pack(15, y, z);
        }
        else if (x == 16) {
            chunk = chunks.get(ChunkPos.toLong(chunk.x + 1, chunk.z));
            key = pack(0, y, z);
        }
        else if (z == -1) {
            chunk = chunks.get(ChunkPos.toLong(chunk.x, chunk.z - 1));
            key = pack(x, y, 15);
        }
        else if (z == 16) {
            chunk = chunks.get(ChunkPos.toLong(chunk.x, chunk.z + 1));
            key = pack(x, y, 0);
        }
        else key = pack(x, y, z);

        return chunk != null && chunk.positions != null && chunk.positions.contains(key);
    }

    private class TChunk {
        private final int x, z;
        public IntSet positions;

        public boolean marked;

        public TChunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.marked = true;
        }

        public void render(Renderer3D renderer) {
            if (positions == null) return;

            // Manual iteration to avoid boxing
            for (IntIterator it = positions.iterator(); it.hasNext();) {
                int pos = it.nextInt();

                int x = getPackedX(pos);
                int y = getPackedY(pos);
                int z = getPackedZ(pos);

                int excludeDir = 0;

                if (connected.get()) {
                    for (Direction dir : DIRECTIONS) {
                        if (chunkContains(this, x + dir.getOffsetX(), y, z + dir.getOffsetZ())) excludeDir |= Dir.get(dir);
                    }
                }

                x += this.x * 16;
                z += this.z * 16;

                renderer.box(x, y, z, x + 1, y + height.get(), z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), excludeDir);
            }
        }

        public long getKey() {
            return ChunkPos.toLong(x, z);
        }
    }

    private static class Context {
        private final World world;

        private Chunk lastChunk;

        public Context() {
            this.world = MeteorClient.mc.world;
        }

        public BlockState get(int x, int y, int z) {
            if (world.isOutOfHeightLimit(y)) return Blocks.VOID_AIR.getDefaultState();

            int cx = x >> 4;
            int cz = z >> 4;

            Chunk chunk;

            if (lastChunk != null && lastChunk.getPos().x == cx && lastChunk.getPos().z == cz) chunk = lastChunk;
            else chunk = world.getChunk(cx, cz, ChunkStatus.FULL, false);

            if (chunk == null) return Blocks.VOID_AIR.getDefaultState();

            ChunkSection section = chunk.getSectionArray()[chunk.getSectionIndex(y)];
            if (section == null) return Blocks.VOID_AIR.getDefaultState();

            lastChunk = chunk;
            return section.getBlockState(x & 15, y & 15, z & 15);
        }
    }

    private enum TunnelSide {
        Walkable,
        PartiallyBlocked,
        FullyBlocked
    }
}
