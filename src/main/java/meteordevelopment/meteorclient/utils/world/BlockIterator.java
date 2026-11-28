/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.world;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockIterator {
    private static final Pool<Callback> callbackPool = new Pool<>(Callback::new);
    private static final List<Callback> callbacks = new ReferenceArrayList<>();

    private static final List<Runnable> afterCallbacks = new ReferenceArrayList<>();

    private static final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
    private static int hRadius, vRadius;

    private static boolean iCareAboutAirBlocks;
    private static boolean disableCurrent;

    private BlockIterator() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(BlockIterator.class);
    }

    @EventHandler(priority = EventPriority.LOWEST - 1)
    private static void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        int px = mc.player.getBlockX();
        int py = mc.player.getBlockY();
        int pz = mc.player.getBlockZ();

        scan(px, py, pz);

        hRadius = 0;
        vRadius = 0;
        iCareAboutAirBlocks = false;

        callbackPool.freeAll(callbacks);
        callbacks.clear();

        for (Runnable callback : afterCallbacks) callback.run();
        afterCallbacks.clear();
    }

    private static void scan(int px, int py, int pz) {
        int x1 = px - hRadius;
        int x2 = px + hRadius;
        int z1 = pz - hRadius;
        int z2 = pz + hRadius;
        int y1 = Math.max(mc.level.getMinY(), py - vRadius);
        int y2 = Math.min(mc.level.getMaxY(), py + vRadius);

        int cx1 = SectionPos.blockToSectionCoord(x1);
        int cx2 = SectionPos.blockToSectionCoord(x2);
        int cz1 = SectionPos.blockToSectionCoord(z1);
        int cz2 = SectionPos.blockToSectionCoord(z2);
        int sy1 = mc.level.getSectionIndex(y1);
        int sy2 = mc.level.getSectionIndex(y2);

        for (int cx = cx1; cx <= cx2; cx++) {
            for (int cz = cz1; cz <= cz2; cz++) {
                LevelChunk chunk = mc.level.getChunk(cx, cz);
                if (chunk.isEmpty()) continue;

                int ox = SectionPos.sectionToBlockCoord(cx);
                int oz = SectionPos.sectionToBlockCoord(cz);

                int ix1 = SectionPos.sectionRelative(Math.max(x1, ox));
                int ix2 = SectionPos.sectionRelative(Math.min(x2, ox + 15));
                int iz1 = SectionPos.sectionRelative(Math.max(z1, oz));
                int iz2 = SectionPos.sectionRelative(Math.min(z2, oz + 15));

                for (int sy = sy1; sy <= sy2; sy++) {
                    LevelChunkSection section = chunk.getSection(sy);
                    if (!iCareAboutAirBlocks && section.hasOnlyAir()) continue;

                    int oy = SectionPos.sectionToBlockCoord(mc.level.getSectionYFromSectionIndex(sy));

                    int iy1 = SectionPos.sectionRelative(Math.max(y1, oy));
                    int iy2 = SectionPos.sectionRelative(Math.min(y2, oy + 15));

                    for (int y = iy1; y <= iy2; y++) {
                        for (int z = iz1; z <= iz2; z++) {
                            for (int x = ix1; x <= ix2; x++) {
                                blockPos.set(ox + x, oy + y, oz + z);
                                BlockState state = section.getBlockState(x, y, z);

                                int dx = Math.abs(ox + x - px);
                                int dy = Math.abs(oy + y - py);
                                int dz = Math.abs(oz + z - pz);

                                for (Iterator<Callback> it = callbacks.iterator(); it.hasNext();) {
                                    Callback callback = it.next();

                                    if (dx <= callback.hRadius && dy <= callback.vRadius && dz <= callback.hRadius) {
                                        disableCurrent = false;
                                        callback.function.accept(blockPos, state);

                                        if (disableCurrent) {
                                            it.remove();
                                            callbackPool.free(callback);

                                            if (callbacks.isEmpty()) {
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void register(int horizontalRadius, int verticalRadius, BiConsumer<BlockPos, BlockState> function) {
        hRadius = Math.max(hRadius, horizontalRadius);
        vRadius = Math.max(vRadius, verticalRadius);

        Callback callback = callbackPool.get();

        callback.function = function;
        callback.hRadius = horizontalRadius;
        callback.vRadius = verticalRadius;

        callbacks.add(callback);
    }

    public static void iCareAboutAirBlocks() {
        iCareAboutAirBlocks = true;
    }

    public static void disableCurrent() {
        disableCurrent = true;
    }

    public static void after(Runnable callback) {
        afterCallbacks.add(callback);
    }

    private static class Callback {
        public BiConsumer<BlockPos, BlockState> function;
        public int hRadius, vRadius;
    }
}
