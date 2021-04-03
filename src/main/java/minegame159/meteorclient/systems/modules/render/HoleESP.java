/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.AbstractBlockAccessor;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockIterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class HoleESP extends Module {
    public enum Mode {
        Flat,
        Box,
        BoxBelow,
        Glow,
        ReverseGlow
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgGlow = settings.createGroup("Glow");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Mode> renderMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("render-mode")
            .description("The rendering mode.")
            .defaultValue(Mode.Glow)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Lines)
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

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("doubles")
            .description("Highlights double holes that can be stood across.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-own")
            .description("Ignores rendering the hole you are currently standing in.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> webs = sgGeneral.add(new BoolSetting.Builder()
            .name("webs")
            .description("Whether to show holes that have webs inside of them.")
            .defaultValue(false)
            .build()
    );

    //Glow

    private final Setting<Double> glowHeight = sgGlow.add(new DoubleSetting.Builder()
            .name("glow-height")
            .description("The height of the glow when Glow mode is active")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Boolean> drawOpposite = sgGlow.add(new BoolSetting.Builder()
            .name("draw-opposite")
            .description("Draws a quad at the opposite end of the glow.")
            .defaultValue(false)
            .build()
    );

    // Colors

    private final Setting<SettingColor> singleBedrock = sgColors.add(new ColorSetting.Builder()
            .name("single-bedrock")
            .description("The color for single holes that are completely bedrock.")
            .defaultValue(new SettingColor(25, 225, 25, 100))
            .build()
    );

    private final Setting<SettingColor> singleObsidian = sgColors.add(new ColorSetting.Builder()
            .name("single-obsidian")
            .description("The color for single holes that are completely obsidian.")
            .defaultValue(new SettingColor(225, 25, 25, 100))
            .build()
    );

    private final Setting<SettingColor> singleMixed = sgColors.add(new ColorSetting.Builder()
            .name("single-mixed")
            .description("The color for single holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(225, 145, 25, 100))
            .build()
    );

    private final Setting<SettingColor> doubleBedrock = sgColors.add(new ColorSetting.Builder()
            .name("double-bedrock")
            .description("The color for double holes that are completely bedrock.")
            .defaultValue(new SettingColor(25, 225, 25, 100))
            .build()
    );

    private final Setting<SettingColor> doubleObsidian = sgColors.add(new ColorSetting.Builder()
            .name("double-obsidian")
            .description("The color for double holes that are completely obsidian.")
            .defaultValue(new SettingColor(225, 25, 25, 100))
            .build()
    );

    private final Setting<SettingColor> doubleMixed = sgColors.add(new ColorSetting.Builder()
            .name("double-mixed")
            .description("The color for double holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(225, 145, 25, 100))
            .build()
    );


    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private final List<Hole> holes = new ArrayList<>();
    private final Color transparent = new Color(0, 0, 0, 0);

    public HoleESP() {
        super(Categories.Render, "hole-esp", "Displays Safe holes that you will take less damage in.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos1, blockState) -> {
            blockPos.set(blockPos1);

            if ((ignoreOwn.get() && (mc.player.getBlockPos().equals(blockPos))) || isBlocked(blockPos)) return;

            if(!webs.get() && mc.world.getBlockState(blockPos).getBlock().is(Blocks.COBWEB)) return;

//            for (Hole hole : holes) {
//                if (hole.blockPos == blockPos
//                        || hole.blockPos == blockPos.north()
//                        || hole.blockPos == blockPos.east()
//                        || hole.blockPos == blockPos.south()
//                        || hole.blockPos == blockPos.west()
//                ) return;
//            }

            Direction currentDir = Direction.UP;
            int bedrocks = 0, obbys = 0, airs = 0;

            BlockState bottom = mc.world.getBlockState(blockPos.down());
            if (bottom.getBlock() == Blocks.BEDROCK) bedrocks++;
            else if (bottom.getBlock() == Blocks.OBSIDIAN) obbys++;
            else if (bottom.isAir()) return;

            BlockState north = mc.world.getBlockState(blockPos.north());
            if (north.getBlock() == Blocks.BEDROCK) bedrocks++;
            else if (north.getBlock() == Blocks.OBSIDIAN) obbys++;
            else if (north.isAir()) {
                currentDir = Direction.NORTH;
                airs++;
            }

            BlockState south = mc.world.getBlockState(blockPos.south());
            if (south.getBlock() == Blocks.BEDROCK) bedrocks++;
            else if (south.getBlock() == Blocks.OBSIDIAN) obbys++;
            else if (south.isAir()) {
                currentDir = Direction.SOUTH;
                airs++;
            }

            BlockState east = mc.world.getBlockState(blockPos.east());
            if (east.getBlock() == Blocks.BEDROCK) bedrocks++;
            else if (east.getBlock() == Blocks.OBSIDIAN) obbys++;
            else if (east.isAir()) {
                currentDir = Direction.EAST;
                airs++;
            }

            BlockState west = mc.world.getBlockState(blockPos.west());
            if (west.getBlock() == Blocks.BEDROCK) bedrocks++;
            else if (west.getBlock() == Blocks.OBSIDIAN) obbys++;
            else if (west.isAir()) {
                currentDir = Direction.WEST;
                airs++;
            }

            if (airs > 1) return;

            if (obbys + bedrocks == 5) {
                if (bedrocks == 5) holes.add(holePool.get().set(blockPos, singleBedrock.get(), Direction.UP));
                else if (obbys == 5) holes.add(holePool.get().set(blockPos, singleObsidian.get(), Direction.UP));
                else holes.add(holePool.get().set(blockPos, singleMixed.get(), Direction.UP));
            }

            else if (obbys + bedrocks == 4 && airs == 1 && doubles.get()) {
                int[] doubleResult = checkArround(blockPos.offset(currentDir), currentDir.getOpposite());

                if (doubleResult[0] == 4 && bedrocks == 4) holes.add(holePool.get().set(blockPos, doubleBedrock.get(), currentDir));
                else if (doubleResult[1] == 4 && obbys == 4) holes.add(holePool.get().set(blockPos, doubleObsidian.get(), currentDir));
                else if (doubleResult[0] + doubleResult[1] == 4) holes.add(holePool.get().set(blockPos, doubleMixed.get(), currentDir));
            }
        });
    }

    private int[] checkArround(BlockPos pos, Direction exclude) {
        int bedrocks = 0, obbys = 0;

        if (isBlocked(pos)) return new int[] {bedrocks, obbys};

        Block bottom = mc.world.getBlockState(pos.down()).getBlock();
        if (bottom == Blocks.BEDROCK) bedrocks++;
        else if (bottom == Blocks.OBSIDIAN) obbys++;

        Block north = mc.world.getBlockState(pos.north()).getBlock();
        if (exclude != Direction.NORTH) {
            if (north == Blocks.BEDROCK) bedrocks++;
            else if (north == Blocks.OBSIDIAN) obbys++;
        }

        Block south = mc.world.getBlockState(pos.south()).getBlock();
        if (south == Blocks.BEDROCK) bedrocks++;
        else if (south == Blocks.OBSIDIAN) obbys++;

        Block east = mc.world.getBlockState(pos.east()).getBlock();
        if (east == Blocks.BEDROCK) bedrocks++;
        else if (east == Blocks.OBSIDIAN) obbys++;

        Block west = mc.world.getBlockState(pos.west()).getBlock();
        if (west == Blocks.BEDROCK) bedrocks++;
        else if (west == Blocks.OBSIDIAN) obbys++;

        return new int[] {bedrocks, obbys};
    }

    private boolean isBlocked(BlockPos pos) {
        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return true;

        for (int i = 0; i < holeHeight.get(); i++) {
            if (((AbstractBlockAccessor) mc.world.getBlockState(pos.up(i)).getBlock()).isCollidable()) return true;
        }

        return false;
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        for (Hole hole : holes) {
            switch (renderMode.get()) {
                case Flat:          drawFlat(hole); break;
                case Box:           drawBox(hole, false); break;
                case BoxBelow:      drawBox(hole, true); break;
                case ReverseGlow:
                case Glow:          drawBoxGlowDirection(hole, (renderMode.get() == Mode.ReverseGlow)); break;
            }
        }
    }

    private void drawFlat(Hole hole) {
        int x = hole.blockPos.getX();
        int y = hole.blockPos.getY();
        int z = hole.blockPos.getZ();
        switch (hole.direction) {
            case UP:    Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x, y, z, 1, hole.colorSides, hole.colorLines, shapeMode.get()); break;
            case NORTH: Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x, y, z + 1, x + 1, z - 1, hole.colorSides, hole.colorLines, shapeMode.get()); break;
            case SOUTH: Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x, y, z, x + 1, z + 2, hole.colorSides, hole.colorLines, shapeMode.get()); break;
            case EAST:  Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x, y, z, x + 2, z + 1, hole.colorSides, hole.colorLines, shapeMode.get()); break;
            case WEST:  Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x + 1, y, z, x - 1, z + 1, hole.colorSides, hole.colorLines, shapeMode.get()); break;
        }
    }

    private void drawBox(Hole hole, boolean down) {
        int x = hole.blockPos.getX();
        int y = down ? hole.blockPos.getY() - 1 : hole.blockPos.getY();
        int z = hole.blockPos.getZ();
        switch (hole.direction) {
            case UP:    Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, down ? hole.blockPos.down() : hole.blockPos, hole.colorSides, hole.colorLines, shapeMode.get(), 0); break;
            case NORTH: Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z + 1, x + 1, y + 1, z - 1, hole.colorSides, hole.colorLines, shapeMode.get(), 0); break;
            case SOUTH: Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, x + 1, y + 1, z + 2, hole.colorSides, hole.colorLines, shapeMode.get(), 0); break;
            case EAST:  Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, x + 2, y + 1, z + 1, hole.colorSides, hole.colorLines, shapeMode.get(), 0); break;
            case WEST:  Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x + 1, y, z, x - 1, y + 1,z + 1, hole.colorSides, hole.colorLines, shapeMode.get(), 0); break;
        }
    }

    private void drawBoxGlowDirection(Hole hole, boolean reverse) {
        int x = hole.blockPos.getX();
        int y = hole.blockPos.getY();
        int z = hole.blockPos.getZ();
        switch (hole.direction) {
            case UP:    drawGlowSimple(x, y, z, x + 1, z + 1, hole.colorSides, hole.colorLines, reverse); break;
            case NORTH: drawGlowSimple(x, y, z + 1, x + 1, z - 1, hole.colorSides, hole.colorLines, reverse); break;
            case SOUTH: drawGlowSimple(x, y, z, x + 1, z + 2, hole.colorSides, hole.colorLines, reverse); break;
            case EAST:  drawGlowSimple(x, y, z, x + 2, z + 1, hole.colorSides, hole.colorLines, reverse); break;
            case WEST:  drawGlowSimple(x + 1, y, z, x - 1, z + 1, hole.colorSides, hole.colorLines, reverse); break;
        }
    }

    private void drawGlowSimple(double x1, double y, double z1, double x2, double z2, Color colorSides, Color colorLines, boolean reverse) {
        if (shapeMode.get() != ShapeMode.Lines) Renderer.NORMAL.gradientBoxSides(x1, y, z1, x2, z2, glowHeight.get(), colorLines, transparent, reverse);
        if (shapeMode.get() != ShapeMode.Sides) {
            if (drawOpposite.get()) Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x1, reverse ? y : y + glowHeight.get(), z1, x2, z2, colorSides, colorLines, ShapeMode.Lines);
            Renderer.quadWithLinesHorizontal(Renderer.NORMAL, Renderer.LINES, x1, reverse ? y + glowHeight.get() : y, z1, x2, z2, colorSides, colorLines, ShapeMode.Lines);
            Renderer.LINES.gradientVerticalBox(x1, y, z1, x2, z2, glowHeight.get(), colorLines, transparent, reverse);
        }
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public Color colorSides = new Color();
        public Color colorLines = new Color();
        public Direction direction;

        public Hole set(BlockPos blockPos, Color color, Direction direction) {
            this.blockPos.set(blockPos);
            this.direction = direction;

            colorLines.set(color);
            colorSides.set(color);
            colorSides.a *= 0.5;
            colorSides.validate();

            return this;
        }
    }
}
