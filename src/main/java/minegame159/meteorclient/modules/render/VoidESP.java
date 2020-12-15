package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Dimension;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class VoidESP extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Colors");


    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(64)
            .min(0)
            .sliderMax(256)
            .build()
    );

    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder()
            .name("hole-height")
            .description("Minimum hole height required to be rendered.")
            .defaultValue(1)  // if we already have one hole in bedrock layer, there is already something interesting
            .min(1)
            .sliderMax(5)     // no sense to check more then 5
            .build()
    );

    private final Setting<Boolean> fill = sgRender.add(new BoolSetting.Builder()
            .name("fill")
            .description("Fill the shapes rendered.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Color> fillColor = sgRender.add(new ColorSetting.Builder()
            .name("fill-color")
            .description("Color to fill holes in the void.")
            .defaultValue(new Color(225, 25, 25))
            .build()
    );

    private final Setting<Color> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color to draw lines of holes in the void.")
            .defaultValue(new Color(225, 25, 25))
            .build()
    );


    public VoidESP() {
        super(Category.Render, "void-esp", "Renders holes in bedrock layers.");
    }

    private List<BlockPos> voidHoles = new ArrayList<>();

    private void getHoles(int searchRange, int holeHeight) {
        voidHoles.clear();

        BlockPos playerPos = mc.player.getBlockPos();
        int playerY = playerPos.getY();

        for (int x = -searchRange; x < searchRange; ++x) {
            for (int z = -searchRange; z < searchRange; ++z) {
                BlockPos bottomBlockPos = playerPos.add(x, -playerY, z);

                int blocksFromBottom = 0;
                for (int i = 0; i < holeHeight; ++i)
                    if (mc.world.getBlockState(bottomBlockPos.add(0, i, 0)).getBlock() != Blocks.BEDROCK)
                        ++blocksFromBottom;

                if (blocksFromBottom >= holeHeight) voidHoles.add(bottomBlockPos);

                // checking nether roof
                if (Utils.getDimension() == Dimension.Nether) {
                    BlockPos topBlockPos = playerPos.add(x, 127 - playerY, z);

                    int blocksFromTop = 0;
                    for (int i = 0; i < holeHeight; ++i)
                        if (mc.world.getBlockState(bottomBlockPos.add(0, 127 - i, 0)).getBlock() != Blocks.BEDROCK)
                            ++blocksFromTop;

                    if (blocksFromTop >= holeHeight) voidHoles.add(topBlockPos);
                }
            }
        }
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        getHoles(horizontalRadius.get().intValue(), holeHeight.get().intValue());
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
