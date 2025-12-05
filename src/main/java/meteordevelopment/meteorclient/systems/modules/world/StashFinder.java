/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.entity.*;
import net.minecraft.item.Items;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StashFinder extends Module {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<ChunkPos, Vec3d> tracePositions = new HashMap<>();
    private final Set<ChunkPos> disabledTracePositions = new HashSet<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
        .name("storage-blocks")
        .description("Select the storage blocks to search for.")
        .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
        .build()
    );

    private final Setting<Integer> minimumStorageCount = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-storage-count")
        .description("The minimum amount of storage blocks in a chunk to record the chunk.")
        .defaultValue(4)
        .min(1)
        .sliderMin(1)
        .build()
    );

    private final Setting<List<Block>> blacklistedBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blacklisted-support-blocks")
        .description("Blocks that prevent counting a storage block entity when it sits on them.")
        .defaultValue(new ArrayList<>())
        .build()
    );

    private final Setting<Integer> minimumDistance = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-distance")
        .description("The minimum distance you must be from spawn to record a certain chunk.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10000)
        .build()
    );

    private final Setting<Boolean> sendNotifications = sgGeneral.add(new BoolSetting.Builder()
        .name("notifications")
        .description("Sends Minecraft notifications when new stashes are found.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Mode> notificationMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("notification-mode")
        .description("The mode to use for notifications.")
        .defaultValue(Mode.Both)
        .visible(sendNotifications::get)
        .build()
    );

    private final Setting<Boolean> renderTrace = sgRender.add(new BoolSetting.Builder()
        .name("render-trace")
        .description("Renders a tracer to the last found stash.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> traceColor = sgRender.add(new ColorSetting.Builder()
        .name("trace-color")
        .description("Color of the stash tracer.")
        .defaultValue(new SettingColor(255, 215, 0, 255))
        .visible(renderTrace::get)
        .build()
    );

    private final Setting<Boolean> renderTraceColumn = sgRender.add(new BoolSetting.Builder()
        .name("render-trace-column")
        .description("Renders a vertical column at the center of traced chunks.")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> traceColumnColor = sgRender.add(new ColorSetting.Builder()
        .name("trace-column-color")
        .description("Color of the stash tracer column.")
        .defaultValue(new SettingColor(255, 215, 0, 100))
        .visible(renderTraceColumn::get)
        .build()
    );

    private final Setting<Integer> traceArrivalDistance = sgRender.add(new IntSetting.Builder()
        .name("trace-arrival-distance")
        .description("Hide the trace when you are this close to the stash.")
        .defaultValue(16)
        .min(1)
        .sliderMin(1)
        .sliderMax(50)
        .visible(renderTrace::get)
        .build()
    );

    private final Setting<Integer> traceMaxDistance = sgRender.add(new IntSetting.Builder()
        .name("trace-max-distance")
        .description("Hide the trace when you are farther than this distance from the stash.")
        .defaultValue(2000)
        .min(10)
        .sliderMin(50)
        .sliderMax(10000)
        .visible(renderTrace::get)
        .build()
    );

    private final Setting<Keybind> clearTracesBind = sgRender.add(new KeybindSetting.Builder()
        .name("clear-traces-bind")
        .description("Keybind to clear all stash traces.")
        .defaultValue(Keybind.none())
        .build()
    );

    public List<Chunk> chunks = new ArrayList<>();

    public StashFinder() {
        super(Categories.World, "stash-finder", "Searches loaded chunks for storage blocks. Saves to <your minecraft folder>/meteor-client");
    }

    @Override
    public void onActivate() {
        load();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!clearTracesBind.get().isPressed()) return;
        tracePositions.clear();
        disabledTracePositions.clear();
    }

    @EventHandler
    private void onChunkData(ChunkDataEvent event) {
        // Check the distance.
        double chunkXAbs = Math.abs(event.chunk().getPos().x * 16);
        double chunkZAbs = Math.abs(event.chunk().getPos().z * 16);
        if (Math.sqrt(chunkXAbs * chunkXAbs + chunkZAbs * chunkZAbs) < minimumDistance.get()) return;

        Chunk chunk = new Chunk(event.chunk().getPos());

        List<Block> blockBlacklist = blacklistedBlocks.get();

        for (BlockEntity blockEntity : event.chunk().getBlockEntities().values()) {
            BlockEntityType<?> type = blockEntity.getType();

            if (!storageBlocks.get().contains(type)) continue;

            if (!blockBlacklist.isEmpty()) {
                BlockPos below = blockEntity.getPos().down();
                if (blockBlacklist.contains(event.chunk().getBlockState(below).getBlock())) continue;
            }

            if (blockEntity instanceof ChestBlockEntity) chunk.chests++;
            else if (blockEntity instanceof BarrelBlockEntity) chunk.barrels++;
            else if (blockEntity instanceof ShulkerBoxBlockEntity) chunk.shulkers++;
            else if (blockEntity instanceof EnderChestBlockEntity) chunk.enderChests++;
            else if (blockEntity instanceof AbstractFurnaceBlockEntity) chunk.furnaces++;
            else if (blockEntity instanceof DispenserBlockEntity) chunk.dispensersDroppers++;
            else if (blockEntity instanceof HopperBlockEntity) chunk.hoppers++;
        }

        if (chunk.getTotal() >= minimumStorageCount.get()) {
            Chunk prevChunk = null;
            int i = chunks.indexOf(chunk);

            if (i < 0) chunks.add(chunk);
            else prevChunk = chunks.set(i, chunk);

            double y = mc.player != null ? mc.player.getEyeY() : 0.0;
            if (!disabledTracePositions.contains(chunk.chunkPos)) {
                tracePositions.put(chunk.chunkPos, new Vec3d(chunk.x, y, chunk.z));
            }

            saveJson();
            saveCsv();

            if (sendNotifications.get() && (!chunk.equals(prevChunk) || !chunk.countsEqual(prevChunk))) {
                switch (notificationMode.get()) {
                    case Chat -> sendChatNotification(chunk);
                    case Toast -> {
                        MeteorToast toast = new MeteorToast.Builder(title).icon(Items.CHEST).text("Found Stash!").build();
                        mc.getToastManager().add(toast);
                    }
                    case Both -> {
                        sendChatNotification(chunk);
                        MeteorToast toast = new MeteorToast.Builder(title).icon(Items.CHEST).text("Found Stash!").build();
                        mc.getToastManager().add(toast);
                    }
                }
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        // Sort
        chunks.sort(Comparator.comparingInt(value -> -value.getTotal()));

        WVerticalList list = theme.verticalList();

        // Clear
        WButton clear = list.add(theme.button("Clear")).widget();
        WButton clearTraces = list.add(theme.button("Clear Traces")).widget();

        WTable table = new WTable();
        if (!chunks.isEmpty()) list.add(table);

        clear.action = () -> {
            chunks.clear();
            table.clear();
            tracePositions.clear();
            disabledTracePositions.clear();
        };

        clearTraces.action = () -> {
            tracePositions.clear();
            disabledTracePositions.clear();
            table.clear();
            fillTable(theme, table);
        };

        // Chunks
        fillTable(theme, table);

        return list;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        for (Chunk chunk : chunks) {
            table.add(theme.label("Pos: " + chunk.x + ", " + chunk.z));
            table.add(theme.label("Total: " + chunk.getTotal()));

            WButton open = table.add(theme.button("Open")).widget();
            open.action = () -> mc.setScreen(new ChunkScreen(theme, chunk));

            WButton gotoBtn = table.add(theme.button("Goto")).widget();
            gotoBtn.action = () -> PathManagers.get().moveTo(new BlockPos(chunk.x, 0, chunk.z), true);

            WButton traceToggle = table.add(theme.button("Marker:")).widget();
            updateMarkerButton(traceToggle, tracePositions.containsKey(chunk.chunkPos));
            traceToggle.action = () -> {
                if (tracePositions.containsKey(chunk.chunkPos)) {
                    tracePositions.remove(chunk.chunkPos);
                    disabledTracePositions.add(chunk.chunkPos);
                } else {
                    double y = mc.player != null ? mc.player.getEyeY() : 0.0;
                    tracePositions.put(chunk.chunkPos, new Vec3d(chunk.x, y, chunk.z));
                    disabledTracePositions.remove(chunk.chunkPos);
                }
                updateMarkerButton(traceToggle, tracePositions.containsKey(chunk.chunkPos));
            };

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                if (chunks.remove(chunk)) {
                    tracePositions.remove(chunk.chunkPos);
                    disabledTracePositions.remove(chunk.chunkPos);
                    table.clear();
                    fillTable(theme, table);

                    saveJson();
                    saveCsv();
                }
            };

            table.row();
        }
    }

    private void load() {
        boolean loaded = false;

        // Try to load json
        File file = getJsonFile();
        if (file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                chunks = GSON.fromJson(reader, new TypeToken<List<Chunk>>() {}.getType());
                reader.close();

                for (Chunk chunk : chunks) chunk.calculatePos();

                loaded = true;
            } catch (Exception ignored) {
                if (chunks == null) chunks = new ArrayList<>();
            }
        }

        // Try to load csv
        file = getCsvFile();
        if (!loaded && file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                reader.readLine();

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = line.split(" ");
                    Chunk chunk = new Chunk(new ChunkPos(Integer.parseInt(values[0]), Integer.parseInt(values[1])));

                    chunk.chests = Integer.parseInt(values[2]);
                    chunk.shulkers = Integer.parseInt(values[3]);
                    chunk.enderChests = Integer.parseInt(values[4]);
                    chunk.furnaces = Integer.parseInt(values[5]);
                    chunk.dispensersDroppers = Integer.parseInt(values[6]);
                    chunk.hoppers = Integer.parseInt(values[7]);

                    chunks.add(chunk);
                }

                reader.close();
            } catch (Exception ignored) {
                if (chunks == null) chunks = new ArrayList<>();
            }
        }
    }

    private void saveCsv() {
        try {
            File file = getCsvFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);

            writer.write("X,Z,Chests,Barrels,Shulkers,EnderChests,Furnaces,DispensersDroppers,Hoppers\n");
            for (Chunk chunk : chunks) chunk.write(writer);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveJson() {
        try {
            File file = getJsonFile();
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(chunks, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getJsonFile() {
        return new File(new File(new File(MeteorClient.FOLDER, "stashes"), Utils.getFileWorldName()), "stashes.json");
    }

    private File getCsvFile() {
        return new File(new File(new File(MeteorClient.FOLDER, "stashes"), Utils.getFileWorldName()), "stashes.csv");
    }

    @Override
    public String getInfoString() {
        return String.valueOf(chunks.size());
    }

    private void sendChatNotification(Chunk chunk) {
        TextColor meteorPurple = TextColor.fromRgb(MeteorClient.ADDON.color.getPacked());
        MutableText coords = Text.literal(chunk.x + ", " + chunk.z)
            .setStyle(Style.EMPTY
                .withColor(meteorPurple)
                .withFormatting(Formatting.UNDERLINE)
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Path to stash")))
                .withClickEvent(new RunnableClickEvent(() -> PathManagers.get().moveTo(new BlockPos(chunk.x, 0, chunk.z), true))));

        MutableText message = Text.literal("Found stash at ")
            .formatted(Formatting.GRAY)
            .append(Text.literal("[").formatted(Formatting.GRAY))
            .append(coords)
            .append(Text.literal("]").formatted(Formatting.GRAY))
            .append(Text.literal(".").formatted(Formatting.GRAY));

        ChatUtils.sendMsg(message);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (tracePositions.isEmpty() || mc.player == null) return;

        double eyeY = mc.player.getEyeY();
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        int bottomY = mc.world.getBottomY();
        int topY = bottomY + mc.world.getDimension().height();

        tracePositions.entrySet().removeIf(entry -> {
            Vec3d pos = entry.getValue();
            double horizontalDist = Math.hypot(pos.x - playerX, pos.z - playerZ);
            return horizontalDist <= traceArrivalDistance.get();
        });

        if (!renderTrace.get() && !renderTraceColumn.get()) return;

        for (Vec3d pos : tracePositions.values()) {
            double horizontalDist = Math.hypot(pos.x - playerX, pos.z - playerZ);
            if (horizontalDist > traceMaxDistance.get()) continue;

            if (renderTrace.get()) {
                event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, pos.x, eyeY, pos.z, traceColor.get());
            }

            if (renderTraceColumn.get()) {
                double x1 = pos.x - 0.5;
                double x2 = pos.x + 0.5;
                double z1 = pos.z - 0.5;
                double z2 = pos.z + 0.5;

                event.renderer.line(x1, bottomY, z1, x1, topY, z1, traceColumnColor.get());
                event.renderer.line(x1, bottomY, z2, x1, topY, z2, traceColumnColor.get());
                event.renderer.line(x2, bottomY, z1, x2, topY, z1, traceColumnColor.get());
                event.renderer.line(x2, bottomY, z2, x2, topY, z2, traceColumnColor.get());
            }
        }
    }

    private void updateMarkerButton(WButton button, boolean enabled) {
        button.set(enabled ? "Marker: On" : "Marker: Off");
    }

    public enum Mode {
        Chat,
        Toast,
        Both
    }

    public static class Chunk {
        private static final StringBuilder sb = new StringBuilder();

        public ChunkPos chunkPos;
        public transient int x, z;
        public int chests, barrels, shulkers, enderChests, furnaces, dispensersDroppers, hoppers;

        public Chunk(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;

            calculatePos();
        }

        public void calculatePos() {
            x = chunkPos.x * 16 + 8;
            z = chunkPos.z * 16 + 8;
        }

        public int getTotal() {
            return chests + barrels + shulkers + enderChests + furnaces + dispensersDroppers + hoppers;
        }

        public void write(Writer writer) throws IOException {
            sb.setLength(0);
            sb.append(x).append(',').append(z).append(',');
            sb.append(chests).append(',').append(barrels).append(',').append(shulkers).append(',').append(enderChests).append(',').append(furnaces).append(',').append(dispensersDroppers).append(',').append(hoppers).append('\n');
            writer.write(sb.toString());
        }

        public boolean countsEqual(Chunk c) {
            if (c == null) return false;
            return chests != c.chests || barrels != c.barrels || shulkers != c.shulkers || enderChests != c.enderChests || furnaces != c.furnaces || dispensersDroppers != c.dispensersDroppers || hoppers != c.hoppers;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Chunk chunk = (Chunk) o;
            return Objects.equals(chunkPos, chunk.chunkPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chunkPos);
        }
    }

    private static class ChunkScreen extends WindowScreen {
        private final Chunk chunk;

        public ChunkScreen(GuiTheme theme, Chunk chunk) {
            super(theme, "Chunk at " + chunk.x + ", " + chunk.z);

            this.chunk = chunk;
        }

        @Override
        public void initWidgets() {
            WTable t = add(theme.table()).expandX().widget();

            // Total
            t.add(theme.label("Total:"));
            t.add(theme.label(chunk.getTotal() + ""));
            t.row();

            t.add(theme.horizontalSeparator()).expandX();
            t.row();

            // Separate
            t.add(theme.label("Chests:"));
            t.add(theme.label(chunk.chests + ""));
            t.row();

            t.add(theme.label("Barrels:"));
            t.add(theme.label(chunk.barrels + ""));
            t.row();

            t.add(theme.label("Shulkers:"));
            t.add(theme.label(chunk.shulkers + ""));
            t.row();

            t.add(theme.label("Ender Chests:"));
            t.add(theme.label(chunk.enderChests + ""));
            t.row();

            t.add(theme.label("Furnaces:"));
            t.add(theme.label(chunk.furnaces + ""));
            t.row();

            t.add(theme.label("Dispensers and droppers:"));
            t.add(theme.label(chunk.dispensersDroppers + ""));
            t.row();

            t.add(theme.label("Hoppers:"));
            t.add(theme.label(chunk.hoppers + ""));
        }
    }
}
