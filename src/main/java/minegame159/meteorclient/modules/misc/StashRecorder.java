package minegame159.meteorclient.modules.misc;

import com.mojang.blaze3d.platform.GlStateManager;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.SaveManager;
import minegame159.meteorclient.events.ChunkDataEvent;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.IntSetting;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import java.util.*;

public class StashRecorder extends Module {
    public static StashRecorder INSTANCE;

    private Setting<Integer> minimumStorageCount = addSetting(new IntSetting.Builder()
            .name("minimum-storage-cont")
            .description("Minimum chest/shulker count required to record that chunk.")
            .defaultValue(4)
            .min(1)
            .build()
    );

    public Map<ChunkPos, Integer> chunkStorageCounts = new HashMap<>();

    public StashRecorder() {
        super(Category.Misc, "stash-recorder", "Searches loaded chunks for chests/shulkers. Saves to <your minecraft folder>/meteor-client/stashes.json");
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
    public WWidget getCustomWidget() {
        // Sort
        List<Map.Entry<ChunkPos, Integer>> entryList = new ArrayList<>(chunkStorageCounts.entrySet());
        entryList.sort(Comparator.comparingInt(value -> -value.getValue()));
        chunkStorageCounts = new LinkedHashMap<>(entryList.size());
        for (Map.Entry<ChunkPos, Integer> entry : entryList) chunkStorageCounts.put(entry.getKey(), entry.getValue());

        WVerticalList list = new WVerticalList(4);

        // Reset
        WHorizontalList topBar = list.add(new WHorizontalList(4));
        WButton reset = topBar.add(new WButton("Reset"));
        WButton saveToFile = topBar.add(new WButton("Save to file"));

        WGrid grid = list.add(new WGrid(8, 4, 3));

        reset.action = () -> {
            chunkStorageCounts.clear();
            grid.clear();
            list.layout();
        };

        saveToFile.action = () -> {
            SaveManager.save(this);
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
