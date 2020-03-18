package minegame159.meteorclient.modules.misc;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.ChunkDataEvent;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Map;

public class StashRecorder extends Module {
    private Setting<Integer> minimumStorageCount = addSetting(new IntSetting.Builder()
            .name("minimum-storage-cont")
            .description("Minimum chest/shulker count required to record that chunk.")
            .defaultValue(4)
            .min(1)
            .build()
    );

    private Map<ChunkPos, Integer> chunkStorageCounts = new HashMap<>();

    public StashRecorder() {
        super(Category.Misc, "stash-recorder", "Searches loaded chunks for chests/shulkers.");
    }

    @EventHandler
    private Listener<ChunkDataEvent> onChunkData = new Listener<>(event -> {
        int storageCount = 0;
        for (BlockEntity blockEntity : event.chunk.getBlockEntities().values()) {
            if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
                storageCount++;
            }
        }

        if (storageCount >= minimumStorageCount.get()) {
            chunkStorageCounts.put(event.chunk.getPos(), storageCount);
        }
    });

    @Override
    public WWidget getCustomWidget() {
        WVerticalList list = new WVerticalList(4);

        // Reset
        WButton reset = list.add(new WButton("Reset"));
        WGrid grid = list.add(new WGrid(8, 4, 3));

        reset.action = () -> {
            chunkStorageCounts.clear();
            grid.clear();
            list.layout();
        };

        // Chunks
        fillGrid(grid);

        return list;
    }

    private void fillGrid(WGrid grid) {
        for (ChunkPos chunkPos : chunkStorageCounts.keySet()) {
            WMinus remove = new WMinus();
            remove.action = () -> {
                chunkStorageCounts.remove(chunkPos);
                grid.clear();
                fillGrid(grid);
                grid.layout();
            };

            grid.addRow(
                    new WLabel("Chunk Pos: " + chunkPos.x + ", " + chunkPos.z),
                    new WLabel("Count: " + chunkStorageCounts.get(chunkPos)),
                    remove
            );
        }
    }
}
