/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ChunkDataEvent;
import minegame159.meteorclient.gui.screens.StashFinderChunkScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.entity.*;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.io.*;
import java.util.*;

public class StashFinder extends ToggleModule {

    public enum Mode {
        Chat,
        Toast
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
            .name("storage-blocks")
            .description("Select storage blocks to search for.")
            .defaultValue(Arrays.asList(StorageBlockListSetting.STORAGE_BLOCKS))
            .build()
    );

    private final Setting<Integer> minimumStorageCount = sgGeneral.add(new IntSetting.Builder()
            .name("minimum-storage-cont")
            .description("Minimum storage block count required to record that chunk.")
            .defaultValue(4)
            .min(1)
            .build()
    );
    
    private final Setting<Integer> minimumDistance = sgGeneral.add(new IntSetting.Builder()
            .name("minimum-distance")
            .description("Minimum distance in blocks from spawn required to record that chunk.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10000)
            .build()
    );

    private final Setting<Boolean> sendNotifications = sgGeneral.add(new BoolSetting.Builder()
            .name("send-notifications")
            .description("Send minecraft notifications when new stashes are found.")
            .defaultValue(true)
            .build()
    );

    private final Setting<StashFinder.Mode> mode = sgGeneral.add(new EnumSetting.Builder<StashFinder.Mode>()
            .name("notification-mode")
            .description("The mode to use for notifications.")
            .defaultValue(Mode.Toast)
            .build()
    );

    public List<Chunk> chunks = new ArrayList<>();

    public StashFinder() {
        super(Category.Misc, "stash-finder", "Searches loaded chunks for storage blocks. Saves to <your minecraft folder>/meteor-client");
    }

    @Override
    public void onActivate() {
        load();
    }

    @EventHandler
    private final Listener<ChunkDataEvent> onChunkData = new Listener<>(event -> {
        // Check distance
        double chunkXAbs = Math.abs(event.chunk.getPos().x * 16);
        double chunkZAbs = Math.abs(event.chunk.getPos().z * 16);
        if (Math.sqrt(chunkXAbs * chunkXAbs + chunkZAbs * chunkZAbs) < minimumDistance.get()) return;

        Chunk chunk = new Chunk(event.chunk.getPos());

        for (BlockEntity blockEntity : event.chunk.getBlockEntities().values()) {
            if (!storageBlocks.get().contains(blockEntity.getType())) continue;

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

            saveJson();
            saveCsv();

            if (sendNotifications.get() && (!chunk.equals(prevChunk) || !chunk.countsEqual(prevChunk))) {
                if (mode.get() == Mode.Toast) {
                    mc.getToastManager().add(new Toast() {
                        private long timer;
                        private long lastTime = -1;

                        @Override
                        public Visibility draw(MatrixStack matrices, ToastManager manager, long currentTime) {
                            if (lastTime == -1) lastTime = currentTime;
                            else timer += currentTime - lastTime;

                            manager.getGame().getTextureManager().bindTexture(new Identifier("textures/gui/toasts.png"));
                            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 255.0F);
                            manager.drawTexture(matrices, 0, 0, 0, 32, 160, 32);

                            manager.getGame().textRenderer.draw(matrices, "StashRecorder found stash.", 12.0F, 12.0F, -11534256);

                            return timer >= 32000 ? Visibility.HIDE : Visibility.SHOW;
                        }
                    });
                } else
                    Chat.info(Formatting.WHITE + "StashRecorder found stash.");
            }
        }
    });

    @Override
    public WWidget getWidget() {
        // Sort
        chunks.sort(Comparator.comparingInt(value -> -value.getTotal()));

        WTable list = new WTable();

        // Clear
        WButton clear = list.add(new WButton("Clear")).getWidget();
        list.row();

        WTable table = new WTable();
        if (chunks.size() > 0) list.add(table);

        clear.action = () -> {
            chunks.clear();
            table.clear();
        };

        // Chunks
        fillTable(table);

        return list;
    }

    private void fillTable(WTable table) {
        for (Chunk chunk : chunks) {
            table.add(new WLabel("Pos: " + chunk.x + ", " + chunk.z));
            table.add(new WLabel("Total: " + chunk.getTotal()));

            WButton open = table.add(new WButton("Open")).getWidget();
            open.action = () -> mc.openScreen(new StashFinderChunkScreen(chunk));

            WButton gotoBtn = table.add(new WButton("Goto")).getWidget();
            gotoBtn.action = () -> BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(chunk.x, chunk.z));

            WMinus remove = table.add(new WMinus()).getWidget();
            remove.action = () -> {
                if (chunks.remove(chunk)) {
                    table.clear();
                    fillTable(table);

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

            writer.write("X,Z,Chests,Shulkers,EnderChests,Furnaces,DispensersDroppers,Hopper\n");
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
        return new File(new File(new File(MeteorClient.FOLDER, "stashes"), Utils.getWorldName()), "stashes.json");
    }

    private File getCsvFile() {
        return new File(new File(new File(MeteorClient.FOLDER, "stashes"), Utils.getWorldName()), "stashes.csv");
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
}
