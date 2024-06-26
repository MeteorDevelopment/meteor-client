/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class SelfTrap extends Module {
    public enum TopMode {
        AntiFacePlace,
        Full,
        Top,
        None
    }

    public enum BottomMode {
        Single,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Which blocks to use.")
        .defaultValue(Blocks.OBSIDIAN, Blocks.NETHERITE_BLOCK)
        .build()
    );

    private final Setting<TopMode> topPlacement = sgGeneral.add(new EnumSetting.Builder<TopMode>()
        .name("top-mode")
        .description("Which positions to place on your top half.")
        .defaultValue(TopMode.Top)
        .build()
    );

    private final Setting<BottomMode> bottomPlacement = sgGeneral.add(new EnumSetting.Builder<BottomMode>()
        .name("bottom-mode")
        .description("Which positions to place on your bottom half.")
        .defaultValue(BottomMode.None)
        .build()
    );

    private final Setting<Integer> delaySetting = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("How many ticks between block placements.")
        .defaultValue(1)
        .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
        .name("center")
        .description("Centers you on the block you are standing on before placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
        .name("turn-off")
        .description("Turns off after placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when placing.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the blocks will be placed.")
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
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 10))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final List<BlockPos> placePositions = new ArrayList<>();
    private boolean placed;
    private int delay;

    public SelfTrap(){
        super(Categories.Combat, "self-trap", "Places blocks above your head.");
    }

    @Override
    public void onActivate() {
        if (!placePositions.isEmpty()) placePositions.clear();
        delay = 0;
        placed = false;

        if (center.get()) PlayerUtils.centerPlayer();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        for (Block currentBlock : blocks.get()) {
            FindItemResult itemResult = InvUtils.findInHotbar(currentBlock.asItem());

            if (turnOff.get() && ((placed && placePositions.isEmpty()) || !itemResult.found())) {
                toggle();
                continue;
            }

            if (!itemResult.found()) {
                placePositions.clear();
                continue;
            }

            findPlacePos(currentBlock);

            if (delay >= delaySetting.get() && !placePositions.isEmpty()) {
                BlockPos blockPos = placePositions.getLast();

                if (BlockUtils.place(blockPos, itemResult, rotate.get(), 50)) {
                    placePositions.remove(blockPos);
                    placed = true;
                }

                delay = 0;
            }
            else delay++;
            return;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placePositions.isEmpty()) return;
        for (BlockPos pos : placePositions) event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void findPlacePos(Block block) {
        placePositions.clear();
        BlockPos pos = mc.player.getBlockPos();

        switch (topPlacement.get()) {
            case Full -> {
                add(pos.add(0, 2, 0), block);
                add(pos.add(1, 1, 0), block);
                add(pos.add(-1, 1, 0), block);
                add(pos.add(0, 1, 1), block);
                add(pos.add(0, 1, -1), block);
            }
            case Top -> add(pos.add(0, 2, 0), block);
            case AntiFacePlace -> {
                add(pos.add(1, 1, 0), block);
                add(pos.add(-1, 1, 0), block);
                add(pos.add(0, 1, 1), block);
                add(pos.add(0, 1, -1), block);
            }
        }

        if (bottomPlacement.get() == BottomMode.Single) add(pos.add(0, -1, 0), block);
    }


    private void add(BlockPos blockPos, Block block) {
        if (!placePositions.contains(blockPos) &&
            mc.world.getBlockState(blockPos).isReplaceable() &&
            mc.world.canPlace(block.getDefaultState(), blockPos, ShapeContext.absent())) placePositions.add(blockPos);
    }
}
