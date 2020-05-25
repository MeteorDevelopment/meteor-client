package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.*;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageESP extends ToggleModule {
    public enum Mode {
        Lines,
        Sides,
        Both
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<BlockEntityType<?>>> storageBlocks = sgGeneral.add(new StorageBlockListSetting.Builder()
            .name("storage-blocks")
            .description("Select storage blocks to display.")
            .defaultValue(Arrays.asList(StorageBlockListSetting.STORAGE_BLOCKS))
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Both)
            .build()
    );

    private final Setting<Color> chest = sgGeneral.add(new ColorSetting.Builder()
            .name("chest")
            .description("Color of chests.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private final Setting<Color> barrel = sgGeneral.add(new ColorSetting.Builder()
            .name("barrel")
            .description("Color of barrels.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private final Setting<Color> shulker = sgGeneral.add(new ColorSetting.Builder()
            .name("chest")
            .description("Color of shulkers.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private final Setting<Color> enderChest = sgGeneral.add(new ColorSetting.Builder()
            .name("ender-chest")
            .description("Color of ender chests.")
            .defaultValue(new Color(120, 0, 255, 255))
            .build()
    );

    private final Setting<Color> other = sgGeneral.add(new ColorSetting.Builder()
            .name("other")
            .description("Color of furnaces, dispenders, droppers and hoppers.")
            .defaultValue(new Color(140, 140, 140, 255))
            .build()
    );

    private final Color lineColor = new Color(0, 0, 0, 0);
    private final Color sideColor = new Color(0, 0, 0, 0);
    private boolean render;
    private int count;

    public StorageESP() {
        super(Category.Render, "storage-esp", "Shows storage blocks.");
    }

    private void getTileEntityColor(BlockEntity blockEntity) {
        render = false;

        if (!storageBlocks.get().contains(blockEntity.getType())) return;

        if (blockEntity instanceof ChestBlockEntity) lineColor.set(chest.get());
        else if (blockEntity instanceof BarrelBlockEntity) lineColor.set(barrel.get());
        else if (blockEntity instanceof ShulkerBoxBlockEntity) lineColor.set(shulker.get());
        else if (blockEntity instanceof EnderChestBlockEntity) lineColor.set(enderChest.get());
        else if (blockEntity instanceof FurnaceBlockEntity || blockEntity instanceof DispenserBlockEntity || blockEntity instanceof HopperBlockEntity) lineColor.set(other.get());
        else return;

        render = true;

        if (mode.get() == Mode.Sides || mode.get() == Mode.Both) {
            sideColor.set(lineColor);
            sideColor.a -= 225;
            if (sideColor.a < 0) sideColor.a = 0;
        }
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        count = 0;

        for (BlockEntity blockEntity : mc.world.blockEntities) {
            getTileEntityColor(blockEntity);
            if (render) {
                double x1 = blockEntity.getPos().getX();
                double y1 = blockEntity.getPos().getY();
                double z1 = blockEntity.getPos().getZ();

                double x2 = blockEntity.getPos().getX() + 1;
                double y2 = blockEntity.getPos().getY() + 1;
                double z2 = blockEntity.getPos().getZ() + 1;

                Direction excludeDir = null;
                if (blockEntity instanceof ChestBlockEntity && blockEntity.getCachedState().get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                    excludeDir = ChestBlock.getFacing(blockEntity.getCachedState());
                }

                if (blockEntity instanceof ChestBlockEntity) {
                    double a = 1.0 / 16.0;

                    if (excludeDir != Direction.WEST) x1 += a;
                    if (excludeDir != Direction.NORTH) z1 += a;

                    if (excludeDir != Direction.EAST) x2 -= a;
                    y2 -= a * 2;
                    if (excludeDir != Direction.SOUTH) z2 -= a;
                }

                if (mode.get() == Mode.Lines) RenderUtils.boxEdges(x1, y1, z1, x2, y2, z2, lineColor, excludeDir);
                else if (mode.get() == Mode.Sides) RenderUtils.boxSides(x1, y1, z1, x2, y2, z2, sideColor, excludeDir);
                else {
                    RenderUtils.boxEdges(x1, y1, z1, x2, y2, z2, lineColor, excludeDir);
                    RenderUtils.boxSides(x1, y1, z1, x2, y2, z2, sideColor, excludeDir);
                }

                count++;
            }
        }
    });

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
