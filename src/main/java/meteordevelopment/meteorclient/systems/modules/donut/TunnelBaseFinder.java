package meteordevelopment.meteorclient.systems.modules.donut;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Finds 1-wide, 2-tall straight tunnels underground: the shape of a player mining
 * through stone. Natural caves are almost never that narrow and that straight.
 */
public class TunnelBaseFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> minLength = sgGeneral.add(new IntSetting.Builder()
        .name("min-length")
        .description("Minimum straight tunnel length inside one chunk.")
        .defaultValue(12)
        .min(5)
        .max(16)
        .sliderRange(5, 16)
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
        .description("Announce chunks with tunnels in chat.")
        .defaultValue(true)
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

    /** Per chunk: tunnel boxes as {minX, minY, minZ, maxX, maxY, maxZ}. */
    private final Long2ObjectMap<List<double[]>> tunnels = new Long2ObjectOpenHashMap<>();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Ember-TunnelBaseFinder");
        thread.setDaemon(true);
        return thread;
    });

    private DimensionType lastDimension;

    public TunnelBaseFinder() {
        super(Categories.Donut, "tunnel-base-finder", "Highlights straight player-dug tunnels underground.");
    }

    @Override
    public void onActivate() {
        synchronized (tunnels) {
            tunnels.clear();
        }
        if (mc.level == null) return;

        lastDimension = mc.level.dimensionType();
        for (ChunkAccess chunk : Utils.chunks()) {
            if (chunk instanceof LevelChunk levelChunk) queueScan(levelChunk);
        }
    }

    @Override
    public void onDeactivate() {
        synchronized (tunnels) {
            tunnels.clear();
        }
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
        worker.submit(() -> {
            if (!isActive()) return;

            List<double[]> found = scan(chunk);
            long key = chunk.getPos().pack();

            synchronized (tunnels) {
                if (found.isEmpty()) {
                    tunnels.remove(key);
                    return;
                }

                boolean isNew = tunnels.put(key, found) == null;
                if (isNew && chatNotify.get()) {
                    double[] first = found.getFirst();
                    int bx = (int) first[0], by = (int) first[1], bz = (int) first[2];
                    mc.execute(() -> info("Tunnel found near %d, %d, %d", bx, by, bz));
                }
            }
        });
    }

    private List<double[]> scan(LevelChunk chunk) {
        List<double[]> found = new ArrayList<>();

        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int bottom = chunk.getMinY() + 1;
        int top = Math.min(maxY.get(), chunk.getMinY() + chunk.getHeight() - 3);
        int needed = minLength.get();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = bottom; y <= top; y++) {
            // Runs along X; side walls are checked on Z, so skip the chunk's edge rows.
            for (int lz = 1; lz < 15; lz++) {
                int run = 0;
                for (int lx = 0; lx <= 16; lx++) {
                    boolean cell = lx < 16 && isTunnelCell(chunk, pos, baseX + lx, y, baseZ + lz, true);
                    if (cell) {
                        run++;
                    } else {
                        if (run >= needed) {
                            found.add(new double[]{baseX + lx - run, y, baseZ + lz, baseX + lx, y + 2, baseZ + lz + 1});
                        }
                        run = 0;
                    }
                }
            }

            // Runs along Z; side walls are checked on X.
            for (int lx = 1; lx < 15; lx++) {
                int run = 0;
                for (int lz = 0; lz <= 16; lz++) {
                    boolean cell = lz < 16 && isTunnelCell(chunk, pos, baseX + lx, y, baseZ + lz, false);
                    if (cell) {
                        run++;
                    } else {
                        if (run >= needed) {
                            found.add(new double[]{baseX + lx, y, baseZ + lz - run, baseX + lx + 1, y + 2, baseZ + lz});
                        }
                        run = 0;
                    }
                }
            }
        }

        return found;
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
        synchronized (tunnels) {
            for (List<double[]> boxes : tunnels.values()) {
                for (double[] b : boxes) {
                    event.renderer.box(b[0], b[1], b[2], b[3], b[4], b[5], sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        synchronized (tunnels) {
            return tunnels.isEmpty() ? null : String.valueOf(tunnels.size());
        }
    }
}
