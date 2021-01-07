/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.PreTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockIterator;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HoleFiller extends Module {

    public enum PlaceMode {
        Obsidian,
        Cobweb,
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

    private final Setting<HoleFiller.PlaceMode> mode = sgGeneral.add(new EnumSetting.Builder<HoleFiller.PlaceMode>()
            .name("block")
            .description("What block to use for hole filling.")
            .defaultValue(PlaceMode.Obsidian)
            .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders an overlay where holes are being filled.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Lines)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(0, 0, 255, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(0, 0, 255, 255))
            .build()
    );

    private final Setting<Integer> renderTimer = sgRender.add(new IntSetting.Builder()
            .name("Timer")
            .description("The amount of time between changing the block render.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();

    public HoleFiller() {
        super(Category.Combat, "hole-filler", "Fills holes with specified blocks.");
    }

    @EventHandler
    private final Listener<PreTickEvent> onTick = new Listener<>(event -> {
        BlockIterator.register(horizontalRadius.get(), verticalRadius.get(), (blockPos1, blockState) -> {
            if (!blockState.getMaterial().isReplaceable()) return;

            blockPos.set(blockPos1);

            for (Iterator<RenderBlock> it = renderBlocks.iterator(); it.hasNext();) {
                RenderBlock renderBlock = it.next();

                if (renderBlock.shouldRemove()) {
                    it.remove();
                    renderBlockPool.free(renderBlock);
                }
            }


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

            if (PlayerUtils.placeBlock(blockPos, findSlot(), Hand.MAIN_HAND)) BlockIterator.disableCurrent();

            Vec3d blockPosition = new Vec3d(blockPos1.getX(), blockPos1.getY(), blockPos1.getZ());

            if (render.get()) {
                RenderBlock renderBlock = renderBlockPool.get();
                renderBlock.reset(blockPosition);
                renderBlocks.add(renderBlock);
            }
        });
    });

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (render.get()) {
            for (RenderBlock renderBlock : renderBlocks) {
                renderBlock.render();
            }
        }
    });

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
                case Any:
                    if (itemStack.getItem() instanceof BlockItem && ((BlockItem) itemStack.getItem()).getBlock().getDefaultState().isFullCube(mc.world, blockPos)) return i;
                    break;
            }
        }

        return -1;
    }

    private BlockPos.Mutable add(int x, int y, int z) {
        blockPos.setX(blockPos.getX() + x);
        blockPos.setY(blockPos.getY() + y);
        blockPos.setZ(blockPos.getZ() + z);
        return blockPos;
    }

    private class RenderBlock {
        private int x, y, z;
        private int timer;

        public void reset(Vec3d pos) {
            x = MathHelper.floor(pos.getX());
            y = MathHelper.floor(pos.getY());
            z = MathHelper.floor(pos.getZ());
            timer = renderTimer.get();
        }

        public boolean shouldRemove() {
            if (timer <= 0) return true;
            timer--;
            return false;
        }

        public void render() {
            Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
