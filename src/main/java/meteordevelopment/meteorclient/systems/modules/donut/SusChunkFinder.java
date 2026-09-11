package meteordevelopment.meteorclient.systems.modules.donut;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.elements.EmberNotificationsHud;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Flags chunks showing signs of player activity, for finding bases.
 *
 * Each signal is a fact about world generation that a natural chunk can't satisfy:
 * terrain only ever places deepslate upright, kelp only grows while its chunk is loaded,
 * and cobbled deepslate or storage blocks don't generate deep underground.
 */
public class SusChunkFinder extends Module {
    private static final String FIND_TYPE = "sus-chunk";

    private final SettingGroup sgSignals = settings.createGroup("Signals");
    private final SettingGroup sgStorage = settings.createGroup("Saving");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Signals

    private final Setting<Boolean> rotatedDeepslate = sgSignals.add(new BoolSetting.Builder()
        .name("rotated-deepslate")
        .description("Deepslate lying sideways. Terrain only generates it upright, so it was placed by a player.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> rotatedThreshold = sgSignals.add(new IntSetting.Builder()
        .name("rotated-threshold")
        .description("Sideways deepslate blocks needed to flag a chunk.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 20)
        .visible(rotatedDeepslate::get)
        .build()
    );

    private final Setting<Boolean> grownKelp = sgSignals.add(new BoolSetting.Builder()
        .name("grown-kelp")
        .description("Tall kelp mostly reaching the surface. Kelp only grows while the chunk is loaded, so someone stays nearby.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> kelpMinColumns = sgSignals.add(new IntSetting.Builder()
        .name("kelp-min-columns")
        .description("Tall kelp columns needed before the kelp signal is considered.")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 64)
        .visible(grownKelp::get)
        .build()
    );

    private final Setting<Integer> kelpMinHeight = sgSignals.add(new IntSetting.Builder()
        .name("kelp-min-height")
        .description("Minimum kelp column height. Filters out shallow water, where reaching the surface is trivial.")
        .defaultValue(8)
        .min(2)
        .sliderRange(2, 25)
        .visible(grownKelp::get)
        .build()
    );

    private final Setting<Double> kelpSurfaceRatio = sgSignals.add(new DoubleSetting.Builder()
        .name("kelp-surface-ratio")
        .description("Fraction of tall kelp columns that must reach the water surface.")
        .defaultValue(0.6)
        .min(0.1)
        .max(1)
        .sliderRange(0.1, 1)
        .visible(grownKelp::get)
        .build()
    );

    private final Setting<Boolean> cobbledDeepslate = sgSignals.add(new BoolSetting.Builder()
        .name("cobbled-deepslate")
        .description("Cobbled deepslate below Y=0, which only comes from mining or placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> cobbledThreshold = sgSignals.add(new IntSetting.Builder()
        .name("cobbled-threshold")
        .description("Cobbled deepslate blocks needed to flag a chunk.")
        .defaultValue(6)
        .min(1)
        .sliderRange(1, 64)
        .visible(cobbledDeepslate::get)
        .build()
    );

    private final Setting<Boolean> undergroundStorage = sgSignals.add(new BoolSetting.Builder()
        .name("underground-storage")
        .description("Shulkers, barrels, furnaces, crafting tables or ender chests deep underground.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> storageThreshold = sgSignals.add(new IntSetting.Builder()
        .name("storage-threshold")
        .description("Storage blocks needed to flag a chunk.")
        .defaultValue(2)
        .min(1)
        .sliderRange(1, 20)
        .visible(undergroundStorage::get)
        .build()
    );

    private final Setting<Integer> storageMaxY = sgSignals.add(new IntSetting.Builder()
        .name("storage-max-y")
        .description("Only count storage at or below this height, so surface villages are ignored.")
        .defaultValue(20)
        .sliderRange(-64, 64)
        .visible(undergroundStorage::get)
        .build()
    );

    // Saving

    private final Setting<Boolean> saveFinds = sgStorage.add(new BoolSetting.Builder()
        .name("save-finds")
        .description("Remember flagged chunks for this server, so they still show after relogging.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoWaypoint = sgStorage.add(new BoolSetting.Builder()
        .name("auto-waypoint")
        .description("Add a waypoint for every newly flagged chunk.")
        .defaultValue(false)
        .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How flagged chunks are drawn.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fill color of flagged chunks.")
        .defaultValue(new SettingColor(170, 120, 255, 45))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color of flagged chunks.")
        .defaultValue(new SettingColor(170, 120, 255, 220))
        .build()
    );

    private final Setting<Boolean> followPlayerY = sgRender.add(new BoolSetting.Builder()
        .name("follow-player-y")
        .description("Draw the chunk plane at your height so it stays visible at any depth.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> renderY = sgRender.add(new IntSetting.Builder()
        .name("render-y")
        .description("Height of the chunk plane.")
        .defaultValue(63)
        .sliderRange(-64, 320)
        .visible(() -> !followPlayerY.get())
        .build()
    );

    private final Setting<Boolean> chatNotify = sgRender.add(new BoolSetting.Builder()
        .name("chat-notify")
        .description("Announce newly flagged chunks in chat.")
        .defaultValue(true)
        .build()
    );

    private final Long2ObjectMap<String> flagged = new Long2ObjectOpenHashMap<>();
    /** Chunks flagged in earlier sessions; drawn even when not loaded. */
    private final LongSet saved = new LongOpenHashSet();
    private final LongSet announced = new LongOpenHashSet();
    private final LongSet pendingRescan = new LongOpenHashSet();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Ember-SusChunkFinder");
        thread.setDaemon(true);
        return thread;
    });

    private final BlockPos.MutableBlockPos abovePos = new BlockPos.MutableBlockPos();
    private DimensionType lastDimension;
    private FindsStorage.Context context = new FindsStorage.Context("", "");
    private int rescanTimer;

    public SusChunkFinder() {
        super(Categories.Donut, "sus-chunk-finder", "Highlights chunks showing signs of player activity.");
    }

    @Override
    public void onActivate() {
        clear();
        if (mc.level == null) return;

        lastDimension = mc.level.dimensionType();
        context = FindsStorage.context();

        synchronized (flagged) {
            for (FindsStorage.Find find : FindsStorage.get(context, FIND_TYPE)) {
                saved.add(ChunkPos.pack(find.x >> 4, find.z >> 4));
            }
        }

        for (ChunkAccess chunk : Utils.chunks()) {
            if (chunk instanceof LevelChunk levelChunk) queueScan(levelChunk);
        }
    }

    @Override
    public void onDeactivate() {
        clear();
    }

    private void clear() {
        synchronized (flagged) {
            flagged.clear();
            saved.clear();
            announced.clear();
            pendingRescan.clear();
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        onActivate();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        queueScan(event.chunk());
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        if (!isRelevant(event.newState) && !isRelevant(event.oldState)) return;

        // Batched: an explosion or a player mining can update hundreds of blocks in one chunk.
        synchronized (flagged) {
            pendingRescan.add(ChunkPos.pack(event.pos));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level == null) return;

        DimensionType dimension = mc.level.dimensionType();
        if (dimension != lastDimension) {
            onActivate();
            return;
        }

        if (++rescanTimer < 20) return;
        rescanTimer = 0;

        long[] keys;
        synchronized (flagged) {
            if (pendingRescan.isEmpty()) return;
            keys = pendingRescan.toLongArray();
            pendingRescan.clear();
        }

        for (long key : keys) {
            ChunkAccess chunk = mc.level.getChunk(ChunkPos.getX(key), ChunkPos.getZ(key), net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
            if (chunk instanceof LevelChunk levelChunk) queueScan(levelChunk);
        }
    }

    private void queueScan(LevelChunk chunk) {
        FindsStorage.Context ctx = context;

        worker.submit(() -> {
            if (!isActive()) return;

            String reason = scan(chunk);
            long key = chunk.getPos().pack();

            boolean isNewFind;
            synchronized (flagged) {
                if (reason == null) {
                    flagged.remove(key);
                    return;
                }

                flagged.put(key, reason);
                // Chunks remembered from earlier sessions were already announced back then.
                isNewFind = !saved.contains(key) && announced.add(key);
                if (isNewFind && saveFinds.get()) saved.add(key);
            }

            if (!isNewFind) return;

            int blockX = chunk.getPos().getMinBlockX() + 8;
            int blockZ = chunk.getPos().getMinBlockZ() + 8;
            BlockPos center = new BlockPos(blockX, 64, blockZ);

            if (saveFinds.get()) FindsStorage.add(ctx, FIND_TYPE, center, reason);

            mc.execute(() -> {
                if (autoWaypoint.get()) FindsStorage.waypoint("Sus chunk", center);
                if (chatNotify.get()) info("Sus chunk at (highlight)%d, %d(default): %s", blockX, blockZ, reason);
                EmberNotificationsHud.push("Sus chunk found", blockX + ", " + blockZ, EmberNotificationsHud.GOOD);
            });
        });
    }

    /** @return why the chunk is suspicious, or null if it looks natural */
    private String scan(LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int minY = chunk.getMinY();
        int maxStorageY = storageMaxY.get();

        int rotated = 0;
        int cobbled = 0;
        int storage = 0;
        boolean ancientCity = false;

        int[] kelpBottom = new int[256];
        int[] kelpTop = new int[256];
        java.util.Arrays.fill(kelpBottom, Integer.MAX_VALUE);
        java.util.Arrays.fill(kelpTop, Integer.MIN_VALUE);
        boolean anyKelp = false;

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(this::isRelevant)) continue;

            int baseY = minY + (i << 4);

            for (int ly = 0; ly < 16; ly++) {
                int y = baseY + ly;

                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        Block block = state.getBlock();

                        if (block == Blocks.DEEPSLATE) {
                            if (state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) rotated++;
                        } else if (block == Blocks.KELP || block == Blocks.KELP_PLANT) {
                            int column = (lx << 4) | lz;
                            if (y < kelpBottom[column]) kelpBottom[column] = y;
                            if (y > kelpTop[column]) kelpTop[column] = y;
                            anyKelp = true;
                        } else if (block == Blocks.COBBLED_DEEPSLATE) {
                            if (y < 0) cobbled++;
                        } else if (block == Blocks.REINFORCED_DEEPSLATE || block == Blocks.SCULK_SHRIEKER) {
                            ancientCity = true;
                        } else if (y <= maxStorageY && isStorage(block)) {
                            storage++;
                        }
                    }
                }
            }
        }

        List<String> reasons = new ArrayList<>(4);

        // Ancient cities contain deepslate variants, cobbled deepslate and storage naturally.
        if (!ancientCity) {
            if (rotatedDeepslate.get() && rotated >= rotatedThreshold.get()) reasons.add(rotated + " rotated deepslate");
            if (cobbledDeepslate.get() && cobbled >= cobbledThreshold.get()) reasons.add(cobbled + " cobbled deepslate");
            if (undergroundStorage.get() && storage >= storageThreshold.get()) reasons.add(storage + " underground storage");
        }

        if (grownKelp.get() && anyKelp) {
            String kelpReason = checkKelp(chunk, kelpBottom, kelpTop);
            if (kelpReason != null) reasons.add(kelpReason);
        }

        return reasons.isEmpty() ? null : String.join(", ", reasons);
    }

    private String checkKelp(LevelChunk chunk, int[] kelpBottom, int[] kelpTop) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int topLimit = chunk.getMinY() + chunk.getHeight() - 1;

        int tallColumns = 0;
        int atSurface = 0;

        for (int column = 0; column < 256; column++) {
            if (kelpTop[column] == Integer.MIN_VALUE) continue;
            if (kelpTop[column] - kelpBottom[column] + 1 < kelpMinHeight.get()) continue;

            tallColumns++;

            int aboveY = kelpTop[column] + 1;
            if (aboveY > topLimit) continue;

            abovePos.set(chunkMinX + (column >> 4), aboveY, chunkMinZ + (column & 15));
            if (chunk.getBlockState(abovePos).getFluidState().isEmpty()) atSurface++;
        }

        if (tallColumns < kelpMinColumns.get()) return null;
        if ((double) atSurface / tallColumns < kelpSurfaceRatio.get()) return null;

        return atSurface + "/" + tallColumns + " kelp grown to surface";
    }

    private boolean isRelevant(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DEEPSLATE
            || block == Blocks.KELP
            || block == Blocks.KELP_PLANT
            || block == Blocks.COBBLED_DEEPSLATE
            || block == Blocks.REINFORCED_DEEPSLATE
            || block == Blocks.SCULK_SHRIEKER
            || isStorage(block);
    }

    private static boolean isStorage(Block block) {
        return block instanceof ShulkerBoxBlock
            || block == Blocks.BARREL
            || block == Blocks.FURNACE
            || block == Blocks.CRAFTING_TABLE
            || block == Blocks.ENDER_CHEST;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null) return;

        double y = followPlayerY.get() ? Math.floor(mc.player.getY()) : renderY.get();

        synchronized (flagged) {
            for (long key : flagged.keySet()) drawChunk(event, key, y);
            for (long key : saved) {
                if (!flagged.containsKey(key)) drawChunk(event, key, y);
            }
        }
    }

    private void drawChunk(Render3DEvent event, long key, double y) {
        double x = (double) ChunkPos.getX(key) * 16;
        double z = (double) ChunkPos.getZ(key) * 16;
        event.renderer.box(x, y, z, x + 16, y, z + 16, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        synchronized (flagged) {
            LongSet all = new LongOpenHashSet(flagged.keySet());
            all.addAll(saved);
            return all.isEmpty() ? null : String.valueOf(all.size());
        }
    }
}
