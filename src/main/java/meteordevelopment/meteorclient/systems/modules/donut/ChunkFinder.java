package meteordevelopment.meteorclient.systems.modules.donut;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
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
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tells freshly generated chunks apart from ones players have already loaded.
 *
 * When the server generates a chunk, its water and lava start flowing and the server
 * sends those flow updates right after the chunk. A chunk whose liquids are already
 * flowing in the data it was sent was generated before, so someone has been there.
 */
public class ChunkFinder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> newChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("new-chunks")
        .description("Highlight chunks that were just generated (nobody has been there).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> oldChunks = sgGeneral.add(new BoolSetting.Builder()
        .name("old-chunks")
        .description("Highlight chunks that were generated before (someone has been there).")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How chunks are drawn.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> newColor = sgRender.add(new ColorSetting.Builder()
        .name("new-color")
        .description("Color of new chunks.")
        .defaultValue(new SettingColor(255, 80, 80, 45))
        .visible(newChunks::get)
        .build()
    );

    private final Setting<SettingColor> oldColor = sgRender.add(new ColorSetting.Builder()
        .name("old-color")
        .description("Color of old chunks.")
        .defaultValue(new SettingColor(90, 220, 120, 45))
        .visible(oldChunks::get)
        .build()
    );

    private final LongSet newSet = new LongOpenHashSet();
    private final LongSet oldSet = new LongOpenHashSet();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Ember-ChunkFinder");
        thread.setDaemon(true);
        return thread;
    });

    private DimensionType lastDimension;

    public ChunkFinder() {
        super(Categories.Donut, "chunk-finder", "Highlights new and previously visited chunks.");
    }

    @Override
    public void onActivate() {
        synchronized (newSet) {
            newSet.clear();
            oldSet.clear();
        }
        if (mc.level == null) return;

        lastDimension = mc.level.dimensionType();
        for (ChunkAccess chunk : Utils.chunks()) {
            if (chunk instanceof LevelChunk levelChunk) queueScan(levelChunk);
        }
    }

    @Override
    public void onDeactivate() {
        synchronized (newSet) {
            newSet.clear();
            oldSet.clear();
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

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundBlockUpdatePacket packet) {
            checkFlowUpdate(packet.getPos(), packet.getBlockState());
        } else if (event.packet instanceof ClientboundSectionBlocksUpdatePacket packet) {
            packet.runUpdates(this::checkFlowUpdate);
        }
    }

    private void checkFlowUpdate(BlockPos pos, BlockState state) {
        if (!isFlowing(state)) return;

        long key = ChunkPos.pack(pos);
        synchronized (newSet) {
            if (!oldSet.contains(key)) newSet.add(key);
        }
    }

    private void queueScan(LevelChunk chunk) {
        worker.submit(() -> {
            if (!isActive()) return;
            if (!hasFlowingLiquid(chunk)) return;

            long key = chunk.getPos().pack();
            synchronized (newSet) {
                if (!newSet.contains(key)) oldSet.add(key);
            }
        });
    }

    private static boolean hasFlowingLiquid(LevelChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null || section.hasOnlyAir() || !section.maybeHas(ChunkFinder::isFlowing)) continue;

            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (isFlowing(section.getBlockState(x, y, z))) return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isFlowing(BlockState state) {
        return !state.getFluidState().isEmpty() && !state.getFluidState().isSource();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null) return;

        double y = Math.floor(mc.player.getY());

        synchronized (newSet) {
            if (newChunks.get()) {
                for (long key : newSet) drawChunk(event, key, y, newColor.get());
            }
            if (oldChunks.get()) {
                for (long key : oldSet) drawChunk(event, key, y, oldColor.get());
            }
        }
    }

    private void drawChunk(Render3DEvent event, long key, double y, SettingColor color) {
        double x = (double) ChunkPos.getX(key) * 16;
        double z = (double) ChunkPos.getZ(key) * 16;
        event.renderer.box(x, y, z, x + 16, y, z + 16, color, color, shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        synchronized (newSet) {
            return newSet.size() + " new, " + oldSet.size() + " old";
        }
    }
}
