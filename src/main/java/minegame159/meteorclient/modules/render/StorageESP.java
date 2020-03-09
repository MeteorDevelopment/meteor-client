package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.ColorSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.block.entity.*;

public class StorageESP extends Module {
    public enum Mode {
        Lines,
        Sides,
        Both
    }
    private Setting<Mode> mode = addSetting(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Both)
            .build()
    );

    private Setting<Color> classic = addSetting(new ColorSetting.Builder()
            .name("classic")
            .description("Color of chests, shulkers and barrels.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private Setting<Color> enderChest = addSetting(new ColorSetting.Builder()
            .name("ender-chest")
            .description("Color of ender chests.")
            .defaultValue(new Color(120, 0, 255, 255))
            .build()
    );

    private Setting<Color> other = addSetting(new ColorSetting.Builder()
            .name("other")
            .description("Color of furnaces, dispenders, droppers and hoppers.")
            .defaultValue(new Color(140, 140, 140, 255))
            .build()
    );

    private Color lineColor = new Color(0, 0, 0, 0);
    private Color sideColor = new Color(0, 0, 0, 0);
    private boolean render;

    public StorageESP() {
        super(Category.Render, "storage-esp", "Shows storage blocks.");
    }

    private void getTileEntityColor(BlockEntity blockEntity) {
        render = true;

        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity || blockEntity instanceof BarrelBlockEntity) lineColor.set(classic.get());
        else if (blockEntity instanceof EnderChestBlockEntity) lineColor.set(enderChest.get());
        else if (blockEntity instanceof FurnaceBlockEntity || blockEntity instanceof DispenserBlockEntity || blockEntity instanceof HopperBlockEntity) lineColor.set(other.get());
        else render = false;

        if (mode.get() == Mode.Sides || mode.get() == Mode.Both) {
            sideColor.set(lineColor);
            sideColor.a -= 225;
            if (sideColor.a < 0) sideColor.a = 0;
        }
    }

    @EventHandler
    private Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (BlockEntity blockEntity : mc.world.blockEntities) {
            getTileEntityColor(blockEntity);
            if (render) {
                int x = blockEntity.getPos().getX();
                int y = blockEntity.getPos().getY();
                int z = blockEntity.getPos().getZ();

                if (mode.get() == Mode.Lines) RenderUtils.blockEdges(x, y, z, lineColor);
                else if (mode.get() == Mode.Sides) RenderUtils.blockSides(x, y, z, sideColor);
                else {
                    RenderUtils.blockEdges(x, y, z, lineColor);
                    RenderUtils.blockSides(x, y, z, sideColor);
                }
            }
        }
    });
}
