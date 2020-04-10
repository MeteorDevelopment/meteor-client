package minegame159.meteorclient.modules.misc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.GlStateManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.ChunkDataEvent;
import minegame159.meteorclient.gui.screens.StashRecorderChunkScreen;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.entity.*;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class StashFinder extends ToggleModule {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Setting<Integer> minimumStorageCount = addSetting(new IntSetting.Builder()
            .name("minimum-storage-cont")
            .description("Minimum storage block count required to record that chunk.")
            .defaultValue(4)
            .min(1)
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
    private Listener<ChunkDataEvent> onChunkData = new Listener<>(event -> {
        Chunk chunk = new Chunk(event.chunk.getPos());

        for (BlockEntity blockEntity : event.chunk.getBlockEntities().values()) {
            if (blockEntity instanceof ChestBlockEntity) chunk.chests++;
            else if (blockEntity instanceof BarrelBlockEntity) chunk.barrels++;
            else if (blockEntity instanceof ShulkerBoxBlockEntity) chunk.shulkers++;
            else if (blockEntity instanceof EnderChestBlockEntity) chunk.enderChests++;
            else if (blockEntity instanceof FurnaceBlockEntity) chunk.furnaces++;
            else if (blockEntity instanceof DispenserBlockEntity) chunk.dispensersDroppers++;
            else if (blockEntity instanceof HopperBlockEntity) chunk.hoppers++;
        }

        if (chunk.getTotal() >= minimumStorageCount.get()) {
            int i = chunks.indexOf(chunk);
            if (i < 0) chunks.add(chunk);
            else chunks.set(i, chunk);

            saveJson();
            saveCsv();

            mc.getToastManager().add(new Toast() {
                private long timer;
                private long lastTime = -1;

                @Override
                public Visibility draw(ToastManager manager, long currentTime) {
                    if (lastTime == -1) lastTime = currentTime;
                    else timer += currentTime - lastTime;

                    manager.getGame().getTextureManager().bindTexture(new Identifier("textures/gui/toasts.png"));
                    GlStateManager.color4f(1.0F, 1.0F, 1.0F, 255.0F);
                    manager.blit(0, 0, 0, 32, 160, 32);

                    manager.getGame().textRenderer.draw("StashRecorder found stash.", 12.0F, 12.0F, -11534256);

                    return timer >= 32000 ? Visibility.HIDE : Visibility.SHOW;
                }
            });
        }
    });

    @Override
    public WWidget getWidget() {
        // Sort
        chunks.sort(Comparator.comparingInt(value -> -value.getTotal()));

        WVerticalList list = new WVerticalList(4);

        // Reset
        WHorizontalList topBar = list.add(new WHorizontalList(4));
        WButton reset = topBar.add(new WButton("Reset"));

        WGrid grid = list.add(new WGrid(8, 4, 5));

        reset.action = () -> {
            chunks.clear();
            grid.clear();
            list.layout();
        };

        // Chunks
        fillGrid(grid);

        return list;
    }

    private void fillGrid(WGrid grid) {
        for (Chunk chunk : chunks) {
            WButton open = new WButton("Open");
            open.action = () -> mc.openScreen(new StashRecorderChunkScreen(chunk));

            WButton gotoBtn = new WButton("Goto");
            gotoBtn.action = () -> BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(chunk.x, chunk.z));

            WMinus remove = new WMinus();
            remove.action = () -> {
                if (chunks.remove(chunk)) {
                    grid.clear();
                    fillGrid(grid);
                    grid.layout();

                    saveJson();
                    saveCsv();
                }
            };

            grid.addRow(
                    new WLabel("Pos: " + chunk.x + ", " + chunk.z),
                    new WLabel("Total: " + chunk.getTotal()),
                    open,
                    gotoBtn,
                    remove
            );
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
