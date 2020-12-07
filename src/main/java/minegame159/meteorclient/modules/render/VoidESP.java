package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class VoidESP extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");


    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(10)
            .min(0)
            .sliderMax(32)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for holes.")
            .defaultValue(32)
            .min(0)
            .sliderMax(255)
            .build()
    );

    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder()
            .name("hole-height")
            .description("Minimum hole height required to be rendered.")
            .defaultValue(5)
            .min(1)
            .build()
    );

    private final Setting<Boolean> fill = sgGeneral.add(new BoolSetting.Builder()
            .name("fill")
            .description("Fill the shapes rendered.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> fillColor = sgColors.add(new ColorSetting.Builder()
            .name("fill-color")
            .description("Color to fill holes in the void.")
            .defaultValue(new Color(225, 25, 25))
            .build()
    );

    private final Setting<Color> lineColor = sgColors.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color to draw lines of holes in the void.")
            .defaultValue(new Color(225, 25, 25))
            .build()
    );


    public VoidESP() {
        super(Category.Render, "void-esp", "Renders holes in bedrock at the bottom of the world.");
    }

    public List<BlockPos> voidHoles = new ArrayList<>();

    public void getHoles(int range) {
        voidHoles.clear();
        BlockPos player = mc.player.getBlockPos();
        for (int y = -Math.min(verticalRadius.get(), player.getY()); y < Math.min(verticalRadius.get(), 255 - player.getY()); ++y) {
            for (int x = -range; x < range; ++x) {
                for (int z = -range; z < range; ++z) {
                    BlockPos pos = player.add(x, y, z);
                    if ((mc.world.getBlockState(pos).getBlock() == Blocks.AIR) && (pos.getY() == 0)) {
                        if (holeHeight.get() > 1) {
                            int positive = 0;
                            for (int i = 1; i < holeHeight.get(); i++) {
                                if (mc.world.getBlockState(pos.add(0, i, 0)).getBlock() != Blocks.BEDROCK) positive++;
                            }
                            if (positive == holeHeight.get() -1) voidHoles.add(pos);
                        } else voidHoles.add(pos);
                    }
                }
            }
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        getHoles(horizontalRadius.get().intValue());
    });

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (BlockPos voidHole : voidHoles) {
            int x = voidHole.getX();
            int y = voidHole.getY();
            int z = voidHole.getZ();
            if (fill.get()) {
                ShapeBuilder.blockSides(x, y, z, fillColor.get(), null);
            }
            ShapeBuilder.blockEdges(x, y, z, lineColor.get(), null);
        }
    });
}