package minegame159.meteorclient.modules.render;

import minegame159.jes.SubscribeEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.builders.ColorSettingBuilder;
import minegame159.meteorclient.settings.builders.EnumSettingBuilder;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import net.minecraft.block.entity.*;

public class StorageESP extends Module {
    public enum Mode {
        Lines,
        Sides,
        Both
    }
    private Setting<Mode> mode = addSetting(new EnumSettingBuilder<Mode>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Both)
            .build()
    );

    private Setting<Color> classic = addSetting(new ColorSettingBuilder()
            .name("classic")
            .description("Color of chests, shulkers and barrels.")
            .defaultValue(new Color(255, 160, 0, 255))
            .build()
    );

    private Setting<Color> enderChest = addSetting(new ColorSettingBuilder()
            .name("ender-chest")
            .description("Color of ender chests.")
            .defaultValue(new Color(120, 0, 255, 255))
            .build()
    );

    private Setting<Color> other = addSetting(new ColorSettingBuilder()
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

        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity || blockEntity instanceof BarrelBlockEntity) lineColor.set(classic.value());
        else if (blockEntity instanceof EnderChestBlockEntity) lineColor.set(enderChest.value());
        else if (blockEntity instanceof FurnaceBlockEntity || blockEntity instanceof DispenserBlockEntity || blockEntity instanceof HopperBlockEntity) lineColor.set(other.value());
        else render = false;

        if (mode.value() == Mode.Sides || mode.value() == Mode.Both) {
            sideColor.set(lineColor);
            sideColor.a -= 225;
            if (sideColor.a < 0) sideColor.a = 0;
        }
    }

    @SubscribeEvent
    private void onRender(RenderEvent e) {
        for (BlockEntity blockEntity : mc.world.blockEntities) {
            getTileEntityColor(blockEntity);
            if (render) {
                int x = blockEntity.getPos().getX();
                int y = blockEntity.getPos().getY();
                int z = blockEntity.getPos().getZ();

                if (mode.value() == Mode.Lines) RenderUtils.blockEdges(x, y, z, lineColor);
                else if (mode.value() == Mode.Sides) RenderUtils.blockSides(x, y, z, sideColor);
                else {
                    RenderUtils.blockEdges(x, y, z, lineColor);
                    RenderUtils.blockSides(x, y, z, sideColor);
                }
            }
        }
    }
}
