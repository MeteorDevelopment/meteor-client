/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixin.AbstractBlockAccessor;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockIterator;
import minegame159.meteorclient.utils.world.BlockUtils;
import minegame159.meteorclient.utils.world.Dir;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HoleFiller extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("blocks")
            .description("Which blocks can be used to fill holes.")
            .defaultValue(Collections.singletonList(Blocks.OBSIDIAN))
            .build()
    );

    private final Setting<Integer> horizontalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> verticalRadius = sgGeneral.add(new IntSetting.Builder()
            .name("vertical-radius")
            .description("Vertical radius in which to search for holes.")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build()
    );


    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("doubles")
            .description("Fills double holes.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The ticks delay between placement.")
            .defaultValue(1)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the holes being filled.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders an overlay where blocks will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color of the target block rendering.")
            .defaultValue(new SettingColor(197, 137, 232))
            .build()
    );

    private final Setting<SettingColor> nextSideColor = sgRender.add(new ColorSetting.Builder()
            .name("next-side-color")
            .description("The side color of the next block to be placed.")
            .defaultValue(new SettingColor(227, 196, 245, 10))
            .build()
    );

    private final Setting<SettingColor> nextLineColor = sgRender.add(new ColorSetting.Builder()
            .name("next-line-color")
            .description("The line color of the next block to be placed.")
            .defaultValue(new SettingColor(227, 196, 245))
            .build()
    );

    private final Pool<Hole> holePool = new Pool<>(Hole::new);
    private final List<Hole> holes = new ArrayList<>();
    private int timer;

    private final byte NULL = 0;

    public HoleFiller() {
        super(Categories.Combat, "hole-filler", "Fills holes with specified blocks.");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Hole hole : holes) holePool.free(hole);
        holes.clear();

        int slot = InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));

        if (slot != -1) {
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

                if (obsidian + bedrock == 5 && air == null) holes.add(holePool.get().set(blockPos, NULL));
                else if (obsidian + bedrock == 8 && doubles.get() && air != null) {
                    holes.add(holePool.get().set(blockPos, Dir.get(air)));
                }
            });
        }
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (timer <= 0 && !holes.isEmpty()) {
            int slot = InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
            BlockUtils.place(holes.get(0).blockPos, Hand.MAIN_HAND, slot, rotate.get(), 0, true);
            timer = placeDelay.get();
        }

        timer--;
    }

    private boolean validHole(BlockPos pos) {
        if (mc.player.getBlockPos().equals(pos)) return false;
        if (((AbstractBlockAccessor) mc.world.getBlockState(pos).getBlock()).isCollidable()) return false;
        return !((AbstractBlockAccessor) mc.world.getBlockState(pos.up()).getBlock()).isCollidable();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(RenderEvent event) {
        if (!render.get()) return;

        for (Hole hole : holes) {
            if (hole == holes.get(0)) Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, hole.blockPos, nextSideColor.get(), nextLineColor.get(), shapeMode.get(), hole.exclude);
            else Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, hole.blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), hole.exclude);
        }
    }

    private static class Hole {
        public BlockPos.Mutable blockPos = new BlockPos.Mutable();
        public byte exclude;

        public Hole set(BlockPos blockPos, byte exclude) {
            this.blockPos.set(blockPos);
            this.exclude = exclude;

            return this;
        }

    }
}
