/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PreTickEvent;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.BlockIterator;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Pool;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class HoleESP extends ToggleModule {

    public enum Mode {
        Flat,
        Box,
        BoxBelow
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Mode> renderMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("render-mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Flat)
            .build()
    );

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
            .defaultValue(10)
            .min(0)
            .sliderMax(32)
            .build()
    );

    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder()
            .name("hole-height")
            .description("Minimum hole height required to be rendered.")
            .defaultValue(3)
            .min(1)
            .build()
    );

    private final Setting<Boolean> ignoreOwn = sgColors.add(new BoolSetting.Builder()
            .name("ignore-own")
            .description("Ignores the hole you are standing in when rendering.")
            .defaultValue(true)
            .build()
    );


    private final Setting<Color> allBedrock = sgColors.add(new ColorSetting.Builder()
            .name("all-bedrock")
            .description("All blocks are bedrock.")
            .defaultValue(new Color(25, 225, 25))
            .build()
    );

    private final Setting<Color> someObsidian = sgColors.add(new ColorSetting.Builder()
            .name("some-obsidian")
            .description("Some blocks are obsidian.")
            .defaultValue(new Color(225, 145, 25))
            .build()
    );

    private final Setting<Color> allObsidian = sgColors.add(new ColorSetting.Builder()
            .name("all-obsidian")
            .description("All blocks are obsidian.")
            .defaultValue(new Color(225, 25, 25))
            .build()
    );

    private final Setting<Boolean> fill = sgColors.add(new BoolSetting.Builder()
            .name("fill")
            .description("Fill the shapes rendered.")
            .defaultValue(true)
            .build()
    );

    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final List<Hole> holes = new ArrayList<>();

    public HoleESP() {
        super(Category.Render, "hole-esp", "Displays holes that you can be in so you don't take explosion damage.");
    }

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> {
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos1, blockState) -> {
            blockPos.set(blockPos1);

            if (!checkHeight()) return;
            if (ignoreOwn.get() && (mc.player.getBlockPos().equals(blockPos))) return;

            Block bottom = mc.world.getBlockState(add(0, -1, 0)).getBlock();
            if (bottom != Blocks.BEDROCK && bottom != Blocks.OBSIDIAN) return;
            Block forward = mc.world.getBlockState(add(0, 1, 1)).getBlock();
            if (forward != Blocks.BEDROCK && forward != Blocks.OBSIDIAN) return;
            Block back = mc.world.getBlockState(add(0, 0, -2)).getBlock();
            if (back != Blocks.BEDROCK && back != Blocks.OBSIDIAN) return;
            Block right = mc.world.getBlockState(add(1, 0, 1)).getBlock();
            if (right != Blocks.BEDROCK && right != Blocks.OBSIDIAN) return;
            Block left = mc.world.getBlockState(add(-2, 0, 0)).getBlock();
            if (left != Blocks.BEDROCK && left != Blocks.OBSIDIAN) return;
            add(1, 0, 0);

            if (bottom == Blocks.BEDROCK && forward == Blocks.BEDROCK && back == Blocks.BEDROCK && right == Blocks.BEDROCK && left == Blocks.BEDROCK) {
                holes.add(holePool.get().set(blockPos, allBedrock.get()));
            } else {
                int obsidian = 0;

                if (bottom == Blocks.OBSIDIAN) obsidian++;
                if (forward == Blocks.OBSIDIAN) obsidian++;
                if (back == Blocks.OBSIDIAN) obsidian++;
                if (right == Blocks.OBSIDIAN) obsidian++;
                if (left == Blocks.OBSIDIAN) obsidian++;

                if (obsidian == 5) holes.add(holePool.get().set(blockPos, allObsidian.get()));
                else holes.add(holePool.get().set(blockPos, someObsidian.get()));
            }
        });
    });

    private boolean checkHeight() {
        if (!mc.world.getBlockState(blockPos).getMaterial().isReplaceable()) return false;

        for (int i = 0; i < holeHeight.get() - 1; i++) {
            if (!mc.world.getBlockState(add(0, 1, 0)).getMaterial().isReplaceable()) return false;
        }

        add(0, -holeHeight.get() + 1, 0);
        return true;
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        for (Hole hole : holes) {
            int x = hole.blockPos.getX();
            int y = hole.blockPos.getY();
            int z = hole.blockPos.getZ();

            switch (renderMode.get()) {
                case Flat:
                    if (fill.get()) ShapeBuilder.quadWithLines(x, y, z, hole.colorSides, hole.colorLines);
                    else ShapeBuilder.emptyQuadWithLines(x, y, z, hole.colorLines);
                    break;
                case Box:
                    if (fill.get()) {
                        ShapeBuilder.blockSides(x, y, z, hole.colorSides, null);
                    }
                    ShapeBuilder.blockEdges(x, y, z, hole.colorLines, null);
                    break;
                case BoxBelow:
                    if (fill.get()) {
                        ShapeBuilder.blockSides(x, y - 1, z, hole.colorSides, null);
                    }
                    ShapeBuilder.blockEdges(x, y - 1, z, hole.colorLines, null);
                    break;
            }
        }
    });

    private BlockPos.Mutable add(int x, int y, int z) {
        blockPos.setX(blockPos.getX() + x);
        blockPos.setY(blockPos.getY() + y);
        blockPos.setZ(blockPos.getZ() + z);
        return blockPos;
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public Color colorSides = new Color();
        public Color colorLines = new Color();

        public Hole set(BlockPos blockPos, Color color) {
            this.blockPos.set(blockPos);
            colorLines.set(color);
            colorSides.set(color);
            colorSides.a -= 175;
            colorSides.validate();
            return this;
        }
    }
}