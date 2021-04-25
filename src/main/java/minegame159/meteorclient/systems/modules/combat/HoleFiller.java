/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockIterator;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class HoleFiller extends Module {
    public enum PlaceMode {
        Obsidian,
        Cobweb,
        Both,
        Any
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

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

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The delay in ticks in between placement.")
            .defaultValue(1)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<PlaceMode> mode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
            .name("block")
            .description("The blocks you use to fill holes with.")
            .defaultValue(PlaceMode.Obsidian)
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
            .description("Renders a block overlay where the obsidian will be placed.")
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
            .defaultValue(new SettingColor(204, 0, 0, 45))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private int tickDelayLeft;

    public HoleFiller() {
        super(Categories.Combat, "hole-filler", "Fills holes with specified blocks.");
    }

    @Override
    public void onActivate() {
        tickDelayLeft = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        int slot = findSlot();

        if (slot != -1 && tickDelayLeft <= 0) {
            tickDelayLeft = placeDelay.get();
            BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos1, blockState) -> {
                if (!blockState.getMaterial().isReplaceable()) return;

                blockPos.set(blockPos1);

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

                if (BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate.get(), 0, true)) {
                    BlockIterator.disableCurrent();
                }
            });
        }
        tickDelayLeft--;
    }

    private int findSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            switch (mode.get()) {
                case Obsidian:
                    if (itemStack.getItem() == Items.OBSIDIAN || itemStack.getItem() == Items.CRYING_OBSIDIAN) return i;
                    break;
                case Cobweb:
                    if (itemStack.getItem() == Items.COBWEB) return i;
                    break;
                case Both:
                    if (itemStack.getItem() == Items.COBWEB || itemStack.getItem() == Items.OBSIDIAN || itemStack.getItem() == Items.CRYING_OBSIDIAN) return i;
                    break;
                case Any:
                    if (itemStack.getItem() instanceof BlockItem && ((BlockItem) itemStack.getItem()).getBlock().getDefaultState().isFullCube(mc.world, blockPos)) return i;
                    break;
            }
        }

        return -1;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(RenderEvent event) {
        if (!render.get() || blockPos == null || blockPos.getY() == -1 || mc.world.getBlockState(blockPos).isAir()) return;
        Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private BlockPos.Mutable add(int x, int y, int z) {
        blockPos.setX(blockPos.getX() + x);
        blockPos.setY(blockPos.getY() + y);
        blockPos.setZ(blockPos.getZ() + z);
        return blockPos;
    }
}
