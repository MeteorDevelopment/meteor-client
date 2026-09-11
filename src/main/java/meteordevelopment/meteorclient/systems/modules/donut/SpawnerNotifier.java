package meteordevelopment.meteorclient.systems.modules.donut;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.BlockUpdateEvent;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpawnerNotifier extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("Notify when a spawner is within this many blocks.")
        .defaultValue(32)
        .min(1)
        .sliderRange(1, 256)
        .build()
    );

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
        .name("render")
        .description("Highlight known spawners.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Spawner highlight color.")
        .defaultValue(new SettingColor(255, 60, 200, 60))
        .visible(render::get)
        .build()
    );

    // Spawners are found once per loaded chunk instead of re-scanning a 65-block cube
    // (274k block lookups) every tick, which tanked the framerate.
    private final Set<BlockPos> spawners = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> announced = ConcurrentHashMap.newKeySet();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Ember-SpawnerNotifier");
        thread.setDaemon(true);
        return thread;
    });

    private int timer;

    public SpawnerNotifier() {
        super(Categories.Donut, "spawner-notifier", "Notifies when spawners are nearby.");
    }

    @Override
    public void onActivate() {
        spawners.clear();
        announced.clear();
        timer = 0;

        if (mc.level == null) return;
        for (ChunkAccess chunk : Utils.chunks()) {
            if (chunk instanceof LevelChunk levelChunk) queueScan(levelChunk);
        }
    }

    @Override
    public void onDeactivate() {
        spawners.clear();
        announced.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        queueScan(event.chunk());
    }

    @EventHandler
    private void onBlockUpdate(BlockUpdateEvent event) {
        BlockPos pos = event.pos.immutable();
        if (event.newState.getBlock() == Blocks.SPAWNER) spawners.add(pos);
        else if (event.oldState.getBlock() == Blocks.SPAWNER) spawners.remove(pos);
    }

    private void queueScan(LevelChunk chunk) {
        worker.submit(() -> {
            if (!isActive()) return;

            LevelChunkSection[] sections = chunk.getSections();
            int baseX = chunk.getPos().getMinBlockX();
            int baseZ = chunk.getPos().getMinBlockZ();

            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                if (section == null || section.hasOnlyAir()) continue;
                if (!section.maybeHas(state -> state.getBlock() == Blocks.SPAWNER)) continue;

                int baseY = chunk.getMinY() + (i << 4);
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            if (section.getBlockState(x, y, z).getBlock() == Blocks.SPAWNER) {
                                spawners.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                            }
                        }
                    }
                }
            }
        });
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;
        if (++timer < 10) return;
        timer = 0;

        BlockPos playerPos = mc.player.blockPosition();
        int maxSq = range.get() * range.get();

        for (BlockPos pos : spawners) {
            if (announced.contains(pos) || playerPos.distSqr(pos) > maxSq) continue;

            announced.add(pos);
            info("Spawner found at %d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        for (BlockPos pos : spawners) {
            event.renderer.box(pos, color.get(), color.get(), ShapeMode.Both, 0);
        }
    }
}
