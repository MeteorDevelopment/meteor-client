package meteordevelopment.meteorclient.systems.modules.donut;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Finds 1-wide, 2-tall straight tunnels underground: the shape of a player mining
 * through stone. Natural caves are almost never that narrow and that straight.
 * Pieces that end on a chunk border are joined with the neighbouring chunk, so long
 * tunnels are found even though each chunk only sees 16 blocks of them.
 */
public class TunnelBaseFinder extends Module {
    private static final String FIND_TYPE = "tunnel";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStorage = settings.createGroup("Saving");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> minLength = sgGeneral.add(new IntSetting.Builder()
        .name("min-length")
        .description("Minimum straight tunnel length, joined across chunk borders.")
        .defaultValue(20)
        .min(5)
        .max(128)
        .sliderRange(5, 64)
        .onChanged(value -> rescan())
        .build()
    );

    private final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("max-y")
        .description("Only look for tunnels at or below this height.")
        .defaultValue(40)
        .sliderRange(-64, 128)
        .build()
    );

    private final Setting<Boolean> chatNotify = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-notify")
        .description("Announce new tunnels in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> saveFinds = sgStorage.add(new BoolSetting.Builder()
        .name("save-finds")
        .description("Remember tunnels for this server, so they still show after relogging.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoWaypoint = sgStorage.add(new BoolSetting.Builder()
        .name("auto-waypoint")
        .description("Add a waypoint at the start of every new tunnel.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How tunnels are drawn.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fill color of tunnels.")
        .defaultValue(new SettingColor(255, 170, 60, 40))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color of tunnels.")
        .defaultValue(new SettingColor(255, 170, 60, 220))
        .build()
    );

    /** A row of blocks a tunnel can run along: fixed is Z for tunnels along X, and X for tunnels along Z. */
    private record Line(boolean alongX, int fixed, int y) {}

    /** Tunnel cells found inside one chunk, start inclusive and end exclusive along the line. */
    private record Run(Line line, int start, int end) {}

    private record Segment(Line line, int start, int end) {
        boolean overlaps(int otherStart, int otherEnd) {
            return start < otherEnd && otherStart < end;
        }
    }

    private final Object lock = new Object();
    private final Long2ObjectMap<List<Run>> runsByChunk = new Long2ObjectOpenHashMap<>();
    private final Map<Line, LongSet> chunksByLine = new HashMap<>();
    private final Map<Line, List<Segment>> tunnels = new HashMap<>();
    /** Ranges already announced per line, so a tunnel that grows as chunks load is only announced once. */
    private final Map<Line, List<int[]>> announced = new HashMap<>();
    private final List<Segment> saved = new ArrayList<>();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Ember-TunnelBaseFinder");
        thread.setDaemon(true);
        return thread;
    });

    /** Bumped on every reset so scans queued before it are thrown away. */
    private volatile int generation;
    private FindsStorage.Context context = new FindsStorage.Context("", "");
    private DimensionType lastDimension;

    public TunnelBaseFinder() {
        super(Categories.Donut, "tunnel-base-finder", "Highlights straight player-dug tunnels underground.");
    }

    @Override
    public void onActivate() {
        generation++;
        clear();
        if (mc.level == null) return;

        lastDimension = mc.level.dimensionType();
        context = FindsStorage.context();

        synchronized (lock) {
            for (FindsStorage.Find find : FindsStorage.get(context, FIND_TYPE)) {
                Segment segment = parse(find);
                if (segment == null) continue;

                saved.add(segment);
                announced.computeIfAbsent(segment.line(), k -> new ArrayList<>()).add(new int[]{segment.start(), segment.end()});
            }
        }

        for (ChunkAccess chunk : Utils.chunks()) {
            if (chunk instanceof LevelChunk levelChunk) queueScan(levelChunk);
        }
    }

    @Override
    public void onDeactivate() {
        generation++;
        clear();
    }

    private void rescan() {
        if (isActive()) onActivate();
    }

    private void clear() {
        synchronized (lock) {
            runsByChunk.clear();
            chunksByLine.clear();
            tunnels.clear();
            announced.clear();
            saved.clear();
        }
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        onActivate();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.level != null && mc.level.dimensionType() != lastDimension) onActivate();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        queueScan(event.chunk());
    }

    private void queueScan(LevelChunk chunk) {
        // Settings and the world name are read here on the render thread, not on the worker.
        int gen = generation;
        int needed = minLength.get();
        int top = maxY.get();
        FindsStorage.Context ctx = context;

        worker.submit(() -> {
            if (!isActive() || gen != generation) return;

            List<Run> runs = scan(chunk, needed, top);
            List<Segment> fresh;

            synchronized (lock) {
                if (gen != generation) return;
                fresh = update(chunk.getPos().pack(), runs, needed);
            }

            for (Segment segment : fresh) announce(ctx, segment);
        });
    }

    /** Replaces one chunk's runs and re-joins every line they touch. Returns tunnels not announced before. */
    private List<Segment> update(long chunkKey, List<Run> runs, int needed) {
        Set<Line> touched = new HashSet<>();

        List<Run> old = runs.isEmpty() ? runsByChunk.remove(chunkKey) : runsByChunk.put(chunkKey, runs);
        if (old != null) {
            for (Run run : old) {
                touched.add(run.line());

                LongSet chunks = chunksByLine.get(run.line());
                if (chunks != null) {
                    chunks.remove(chunkKey);
                    if (chunks.isEmpty()) chunksByLine.remove(run.line());
                }
            }
        }

        for (Run run : runs) {
            touched.add(run.line());
            chunksByLine.computeIfAbsent(run.line(), k -> new LongOpenHashSet()).add(chunkKey);
        }

        List<Segment> fresh = new ArrayList<>();

        for (Line line : touched) {
            List<Segment> joined = join(line, needed);
            if (joined.isEmpty()) {
                tunnels.remove(line);
                continue;
            }
            tunnels.put(line, joined);

            List<int[]> known = announced.computeIfAbsent(line, k -> new ArrayList<>());
            for (Segment segment : joined) {
                boolean seen = false;

                for (int[] range : known) {
                    if (segment.overlaps(range[0], range[1])) {
                        range[0] = Math.min(range[0], segment.start());
                        range[1] = Math.max(range[1], segment.end());
                        seen = true;
                        break;
                    }
                }

                if (!seen) {
                    known.add(new int[]{segment.start(), segment.end()});
                    fresh.add(segment);
                }
            }
        }

        return fresh;
    }

    /** Glues together touching runs from every chunk on the line and keeps the long ones. */
    private List<Segment> join(Line line, int needed) {
        List<Run> runs = new ArrayList<>();

        LongSet chunks = chunksByLine.get(line);
        if (chunks != null) {
            for (long key : chunks) {
                List<Run> list = runsByChunk.get(key);
                if (list == null) continue;

                for (Run run : list) {
                    if (run.line().equals(line)) runs.add(run);
                }
            }
        }

        runs.sort(Comparator.comparingInt(Run::start));

        List<Segment> joined = new ArrayList<>();
        int start = 0, end = Integer.MIN_VALUE;

        for (Run run : runs) {
            if (run.start() <= end) {
                end = Math.max(end, run.end());
                continue;
            }

            if (end - start >= needed) joined.add(new Segment(line, start, end));
            start = run.start();
            end = run.end();
        }
        if (end - start >= needed) joined.add(new Segment(line, start, end));

        return joined;
    }

    private void announce(FindsStorage.Context ctx, Segment segment) {
        Line line = segment.line();
        int length = segment.end() - segment.start();
        String axis = line.alongX() ? "X" : "Z";
        BlockPos pos = line.alongX()
            ? new BlockPos(segment.start(), line.y(), line.fixed())
            : new BlockPos(line.fixed(), line.y(), segment.start());

        if (saveFinds.get()) FindsStorage.add(ctx, FIND_TYPE, pos, axis + ":" + length);

        mc.execute(() -> {
            if (!isActive()) return;

            if (autoWaypoint.get()) FindsStorage.waypoint("Tunnel", pos);
            if (chatNotify.get()) {
                info("Tunnel found: %d blocks along %s at %d, %d, %d", length, axis, pos.getX(), pos.getY(), pos.getZ());
            }
            EmberNotificationsHud.push("Tunnel found", length + " blocks at " + pos.getX() + ", " + pos.getZ(), EmberNotificationsHud.GOOD);
        });
    }

    /** Saved tunnels store their axis and length in the note, as "X:24". */
    private static Segment parse(FindsStorage.Find find) {
        String note = find.note;
        if (note == null || note.length() < 3 || note.charAt(1) != ':') return null;

        int length;
        try {
            length = Integer.parseInt(note.substring(2));
        } catch (NumberFormatException e) {
            return null;
        }
        if (length <= 0) return null;

        boolean alongX = note.charAt(0) == 'X';
        int start = alongX ? find.x : find.z;
        return new Segment(new Line(alongX, alongX ? find.z : find.x, find.y), start, start + length);
    }

    private List<Run> scan(LevelChunk chunk, int needed, int maxHeight) {
        List<Run> runs = new ArrayList<>();

        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int bottom = chunk.getMinY() + 1;
        int top = Math.min(maxHeight, chunk.getMinY() + chunk.getHeight() - 3);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = bottom; y <= top; y++) {
            // Runs along X; side walls are checked on Z, so skip the chunk's edge rows.
            for (int lz = 1; lz < 15; lz++) {
                int run = 0;
                for (int lx = 0; lx <= 16; lx++) {
                    if (lx < 16 && isTunnelCell(chunk, pos, baseX + lx, y, baseZ + lz, true)) {
                        run++;
                        continue;
                    }

                    // Short pieces are only worth keeping if they can continue into the next chunk.
                    if (run > 0 && (run >= needed || lx - run == 0 || lx == 16)) {
                        runs.add(new Run(new Line(true, baseZ + lz, y), baseX + lx - run, baseX + lx));
                    }
                    run = 0;
                }
            }

            // Runs along Z; side walls are checked on X.
            for (int lx = 1; lx < 15; lx++) {
                int run = 0;
                for (int lz = 0; lz <= 16; lz++) {
                    if (lz < 16 && isTunnelCell(chunk, pos, baseX + lx, y, baseZ + lz, false)) {
                        run++;
                        continue;
                    }

                    if (run > 0 && (run >= needed || lz - run == 0 || lz == 16)) {
                        runs.add(new Run(new Line(false, baseX + lx, y), baseZ + lz - run, baseZ + lz));
                    }
                    run = 0;
                }
            }
        }

        return runs;
    }

    /** A 1x2 air gap with solid floor, ceiling and walls on both sides. */
    private static boolean isTunnelCell(LevelChunk chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, boolean alongX) {
        if (!chunk.getBlockState(pos.set(x, y, z)).isAir()) return false;
        if (!chunk.getBlockState(pos.set(x, y + 1, z)).isAir()) return false;
        if (!isSolid(chunk.getBlockState(pos.set(x, y - 1, z)))) return false;
        if (!isSolid(chunk.getBlockState(pos.set(x, y + 2, z)))) return false;

        int sx = alongX ? 0 : 1;
        int sz = alongX ? 1 : 0;

        for (int dy = 0; dy < 2; dy++) {
            if (!isSolid(chunk.getBlockState(pos.set(x + sx, y + dy, z + sz)))) return false;
            if (!isSolid(chunk.getBlockState(pos.set(x - sx, y + dy, z - sz)))) return false;
        }
        return true;
    }

    private static boolean isSolid(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        synchronized (lock) {
            for (List<Segment> segments : tunnels.values()) {
                for (Segment segment : segments) draw(event, segment);
            }
            for (Segment segment : saved) {
                if (!coveredByLive(segment)) draw(event, segment);
            }
        }
    }

    private boolean coveredByLive(Segment segment) {
        List<Segment> live = tunnels.get(segment.line());
        if (live == null) return false;

        for (Segment other : live) {
            if (other.overlaps(segment.start(), segment.end())) return true;
        }
        return false;
    }

    private void draw(Render3DEvent event, Segment segment) {
        Line line = segment.line();

        if (line.alongX()) {
            event.renderer.box(segment.start(), line.y(), line.fixed(), segment.end(), line.y() + 2, line.fixed() + 1,
                sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        } else {
            event.renderer.box(line.fixed(), line.y(), segment.start(), line.fixed() + 1, line.y() + 2, segment.end(),
                sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @Override
    public String getInfoString() {
        synchronized (lock) {
            int count = 0;
            for (List<Segment> segments : tunnels.values()) count += segments.size();
            for (Segment segment : saved) {
                if (!coveredByLive(segment)) count++;
            }
            return count == 0 ? null : String.valueOf(count);
        }
    }
}
