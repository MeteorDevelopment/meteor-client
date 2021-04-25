/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.AbstractBlockAccessor;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.render.color.Color;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockIterator;
import minegame159.meteorclient.utils.world.Dir;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class HoleESP extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(7)
            .min(0)
            .sliderMax(32)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for holes.")
            .defaultValue(7)
            .min(0)
            .sliderMax(32)
            .build()
    );

    private final Setting<Integer> holeHeight = sgGeneral.add(new IntSetting.Builder()
            .name("min-height")
            .description("Minimum hole height required to be rendered.")
            .defaultValue(3)
            .min(1)
            .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("doubles")
            .description("Highlights double holes that can be stood across.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-own")
            .description("Ignores rendering the hole you are currently standing in.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> webs = sgGeneral.add(new BoolSetting.Builder()
            .name("webs")
            .description("Whether to show holes that have webs inside of them.")
            .defaultValue(false)
            .build()
    );

    // Render

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder()
            .name("height")
            .description("The height of rendering.")
            .defaultValue(1)
            .min(0)
            .build()
    );

    private final Setting<Boolean> top = sgRender.add(new BoolSetting.Builder()
            .name("top")
            .description("Whether to render a quad at the top of the hole.")
            .defaultValue(false)
            .build()
    );


    private final Setting<Boolean> bottom = sgRender.add(new BoolSetting.Builder()
            .name("bottom")
            .description("Whether to render a quad at the bottom of the hole.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> bedrockColorTop = sgRender.add(new ColorSetting.Builder()
            .name("bedrock-top")
            .description("The top color for holes that are completely bedrock.")
            .defaultValue(new SettingColor(100, 255, 0, 0))
            .build()
    );

    private final Setting<SettingColor> bedrockColorBottom = sgRender.add(new ColorSetting.Builder()
            .name("bedrock-bottom")
            .description("The bottom color for holes that are completely bedrock.")
            .defaultValue(new SettingColor(100, 255, 0))
            .build()
    );

    private final Setting<SettingColor> obsidianColorTop = sgRender.add(new ColorSetting.Builder()
            .name("obsidian-top")
            .description("The top color for holes that are completely obsidian.")
            .defaultValue(new SettingColor(255, 0, 0, 0))
            .build()
    );

    private final Setting<SettingColor> obsidianColorBottom = sgRender.add(new ColorSetting.Builder()
            .name("obsidian-bottom")
            .description("The bottom color for holes that are completely obsidian.")
            .defaultValue(new SettingColor(255, 0, 0))
            .build()
    );

    private final Setting<SettingColor> mixedColorTop = sgRender.add(new ColorSetting.Builder()
            .name("mixed-top")
            .description("The top color for holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(255, 127, 0, 0))
            .build()
    );

    private final Setting<SettingColor> mixedColorBottom = sgRender.add(new ColorSetting.Builder()
            .name("mixed-bottom")
            .description("The bottom color for holes that have mixed bedrock and obsidian.")
            .defaultValue(new SettingColor(255, 127, 0))
            .build()
    );

    private final MeshBuilder LINES = new MeshBuilder(16384);
    private final MeshBuilder SIDES = new MeshBuilder(16384);

    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final List<Hole> holes = new ArrayList<>();

    private final Color topColor = new Color();
    private final Color bottomColor = new Color();

    private final byte NULL = 0;

    public HoleESP() {
        super(Categories.Render, "hole-esp", "Displays holes that you will take less damage in.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos, blockState) -> {
            if (!validHole(blockPos)) return;

            int bedrock = 0, obsidian = 0;
            Direction air = null;

            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP) continue;

                BlockState state = mc.world.getBlockState(blockPos.offset(direction));

                if (state.getBlock() == Blocks.BEDROCK) bedrock++;
                else if (state.getBlock() == Blocks.OBSIDIAN) obsidian++;
                else if (direction == Direction.DOWN) return;
                else if (validHole(blockPos.offset(direction)) && air == null) {
                    for (Direction dir : Direction.values()) {
                        if (dir == direction.getOpposite() || dir == Direction.UP) continue;

                        BlockState blockState1 = mc.world.getBlockState(blockPos.offset(direction).offset(dir));

                        if (blockState1.getBlock() == Blocks.BEDROCK) bedrock++;
                        else if (blockState1.getBlock() == Blocks.OBSIDIAN) obsidian++;
                        else return;
                    }

                    air = direction;
                }
            }

            if (obsidian + bedrock == 5 && air == null) {
                holes.add(holePool.get().set(blockPos, obsidian == 5 ? Hole.Type.Obsidian : (bedrock == 5 ? Hole.Type.Bedrock : Hole.Type.Mixed), NULL));
            }
            else if (obsidian + bedrock == 8 && doubles.get() && air != null) {
                holes.add(holePool.get().set(blockPos, obsidian == 8 ? Hole.Type.Obsidian : (bedrock == 8 ? Hole.Type.Bedrock : Hole.Type.Mixed), Dir.get(air)));
            }
        });
    }

    private boolean validHole(BlockPos pos) {
        if ((ignoreOwn.get() && (mc.player.getBlockPos().equals(pos)))) return false;

        if (!webs.get() && mc.world.getBlockState(pos).getBlock().is(Blocks.COBWEB)) return false;

        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;

        for (int i = 0; i < holeHeight.get(); i++) {
            if (((AbstractBlockAccessor) mc.world.getBlockState(pos.up(i)).getBlock()).isCollidable()) return false;
        }

        return true;
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        LINES.begin(event, DrawMode.Lines, VertexFormats.POSITION_COLOR);
        SIDES.begin(event, DrawMode.Triangles, VertexFormats.POSITION_COLOR);

        for (HoleESP.Hole hole : holes) {
            int x = hole.blockPos.getX();
            int y = hole.blockPos.getY();
            int z = hole.blockPos.getZ();

            topColor.set(hole.getTopColor());
            bottomColor.set(hole.getBottomColor());

            if (shapeMode.get() != ShapeMode.Lines) drawSides(x, y, z, hole.exclude);
            if (shapeMode.get() != ShapeMode.Sides) drawLines(x, y, z, hole.exclude);
        }

        LINES.end();
        SIDES.end();
    }

    private void drawLines(double x, double y, double z, int excludeDir) {
        if (Dir.is(excludeDir, Dir.WEST) && Dir.is(excludeDir, Dir.NORTH)) LINES.line(x, y, z, x, y + height.get(), z, bottomColor, topColor);
        if (Dir.is(excludeDir, Dir.WEST) && Dir.is(excludeDir, Dir.SOUTH)) LINES.line(x, y, z + 1, x, y + height.get(), z + 1, bottomColor, topColor);
        if (Dir.is(excludeDir, Dir.EAST) && Dir.is(excludeDir, Dir.NORTH)) LINES.line(x + 1, y, z, x + 1, y + height.get(), z, bottomColor, topColor);
        if (Dir.is(excludeDir, Dir.EAST) && Dir.is(excludeDir, Dir.SOUTH)) LINES.line(x + 1, y, z + 1, x + 1, y + height.get(), z + 1, bottomColor, topColor);

        if (Dir.is(excludeDir, Dir.NORTH)) LINES.line(x, y, z, x + 1, y, z, bottomColor);
        if (Dir.is(excludeDir, Dir.NORTH)) LINES.line(x, y + height.get(), z, x + 1, y + height.get(), z, topColor);
        if (Dir.is(excludeDir, Dir.SOUTH)) LINES.line(x, y, z + 1, x + 1, y, z + 1, bottomColor);
        if (Dir.is(excludeDir, Dir.SOUTH)) LINES.line(x, y + height.get(), z + 1, x + 1, y + height.get(), z + 1, topColor);

        if (Dir.is(excludeDir, Dir.WEST)) LINES.line(x, y, z, x, y, z + 1, bottomColor);
        if (Dir.is(excludeDir, Dir.WEST)) LINES.line(x, y + height.get(), z, x, y + height.get(), z + 1, topColor);
        if (Dir.is(excludeDir, Dir.EAST)) LINES.line(x + 1, y, z, x + 1, y, z + 1, bottomColor);
        if (Dir.is(excludeDir, Dir.EAST)) LINES.line(x + 1, y + height.get(), z, x + 1, y + height.get(), z + 1, topColor);
    }

    private void drawSides(double x, double y, double z, int excludeDir) {
        Color color = topColor.copy();
        color.a *= 0.5;

        Color color1 = bottomColor.copy();
        color1.a *= 0.5;

        if (Dir.is(excludeDir, Dir.DOWN) && bottom.get()) SIDES.quad(x, y, z, x, y, z + 1, x + 1, y, z + 1, x + 1, y, z, color1); // Bottom
        if (Dir.is(excludeDir, Dir.UP) && top.get()) SIDES.quad(x, y + height.get(), z, x, y + height.get(), z + 1, x + 1, y + height.get(), z + 1, x + 1, y + height.get(), z, color); // Top

        if (Dir.is(excludeDir, Dir.NORTH)) SIDES.quad(x, y, z, x, y + height.get(), z, x + 1, y + height.get(), z, x + 1, y, z, color1, color); // Front
        if (Dir.is(excludeDir, Dir.SOUTH)) SIDES.quad(x, y, z + 1, x, y + height.get(), z + 1, x + 1, y + height.get(), z + 1, x + 1, y, z + 1, color1, color); // Back

        if (Dir.is(excludeDir, Dir.WEST)) SIDES.quad(x, y, z, x, y + height.get(), z, x, y + height.get(), z + 1, x, y, z + 1,  color1, color); // Left
        if (Dir.is(excludeDir, Dir.EAST)) SIDES.quad(x + 1, y, z, x + 1, y + height.get(), z, x + 1, y + height.get(), z + 1, x + 1, y, z + 1,  color1, color); // Right
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;
        public Type type;

        public Hole set(BlockPos blockPos, Type type, byte exclude) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;
            this.type = type;

            return this;
        }

        public Color getTopColor() {
            switch (this.type) {
                case Obsidian:  return Modules.get().get(HoleESP.class).obsidianColorTop.get();
                case Bedrock:  return Modules.get().get(HoleESP.class).bedrockColorTop.get();
                default:  return Modules.get().get(HoleESP.class).mixedColorTop.get();
            }
        }

        public Color getBottomColor() {
            switch (this.type) {
                case Obsidian:  return Modules.get().get(HoleESP.class).obsidianColorBottom.get();
                case Bedrock:  return Modules.get().get(HoleESP.class).bedrockColorBottom.get();
                default:  return Modules.get().get(HoleESP.class).mixedColorBottom.get();
            }
        }

        public enum Type {
            Bedrock,
            Obsidian,
            Mixed
        }
    }
}
