/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.movement;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.player.ClipAtLedgeEvent;
import minegame159.meteorclient.events.world.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.List;

public class Scaffold extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> safeWalk = sg.add(new BoolSetting.Builder()
            .name("Safe-walk")
            .description("Whether or not to toggle Safe Walk when using Scaffold.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> fastTower = sg.add(new BoolSetting.Builder()
            .name("fast-tower")
            .description("Whether or not to tower up faster.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> radius = sg.add(new IntSetting.Builder()
            .name("radius")
            .description("Radius of your scaffold.")
            .defaultValue(1)
            .min(1)
            .sliderMin(1)
            .sliderMax(7)
            .build()
    );

    private final Setting<List<Block>> blackList = sg.add(new BlockListSetting.Builder()
            .name("blacklist")
            .description("Blacklists certain blocks from being used to Scaffold.")
            .defaultValue(new ArrayList<>())
            .build()
    );

    private final Setting<Boolean> selfToggle = sg.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Toggles when you run out of blocks.")
            .defaultValue(true)
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private BlockState blockState, slotBlockState;
    private int slot, prevSelectedSlot;

    private boolean lastWasSneaking;
    private double lastSneakingY;

    public Scaffold() {
        super(Category.Movement, "Scaffold", "Automatically places blocks under you.");
    }

    @Override
    public void onActivate() {
        lastWasSneaking = mc.player.input.sneaking;
        if (lastWasSneaking) lastSneakingY = mc.player.getY();
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (fastTower.get() && !mc.world.getBlockState(setPos(0, -1, 0)).getMaterial().isReplaceable() && mc.options.keyJump.isPressed() && findSlot(mc.world.getBlockState(setPos(0, -1, 0))) != -1 && mc.player.sidewaysSpeed == 0 &&mc.player.forwardSpeed == 0) mc.player.jump();
        blockState = mc.world.getBlockState(setPos(0, -1, 0));
        if (!blockState.getMaterial().isReplaceable()) return;

        // Go downwards if pressing shift
        boolean lastWasSneaking = this.lastWasSneaking;
        this.lastWasSneaking = mc.player.input.sneaking;
        if (mc.player.input.sneaking) {
            if (!lastWasSneaking) lastSneakingY = mc.player.getY();

            if (lastSneakingY - mc.player.getY() < 0.1) return;
        }

        // Search for block in hotbar
        slot = findSlot(blockState);
        if (slot == -1) return;

        // Change slot
        prevSelectedSlot = mc.player.inventory.selectedSlot;
        mc.player.inventory.selectedSlot = slot;

        PlayerUtils.placeBlock(mc.player.getBlockPos().down(), Hand.MAIN_HAND);
        if (mc.player.input.sneaking) this.lastWasSneaking = false;

        // Place blocks around if radius is bigger than 1
        for (int i = 1; i < radius.get(); i++) {
            int count = 1 + (i - 1) * 2;
            int countHalf = count / 2;

            // Forward
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                PlayerUtils.placeBlock(setPos(j - countHalf, -1, i), Hand.MAIN_HAND);
            }
            // Backward
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                PlayerUtils.placeBlock(setPos(j - countHalf, -1, -i), Hand.MAIN_HAND);
            }
            // Right
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                PlayerUtils.placeBlock(setPos(i, -1, j - countHalf), Hand.MAIN_HAND);
            }
            // Left
            for (int j = 0; j < count; j++) {
                if (!findBlock()) return;
                PlayerUtils.placeBlock(setPos(-i, -1, j - countHalf), Hand.MAIN_HAND);
            }

            // Diagonals
            if (!findBlock()) return;
            PlayerUtils.placeBlock(setPos(-i, -1, i), Hand.MAIN_HAND);
            if (!findBlock()) return;
            PlayerUtils.placeBlock(setPos(i, -1, i), Hand.MAIN_HAND);
            if (!findBlock()) return;
            PlayerUtils.placeBlock(setPos(-i, -1, -i), Hand.MAIN_HAND);
            if (!findBlock()) return;
            PlayerUtils.placeBlock(setPos(i, -1, -i), Hand.MAIN_HAND);
        }

        // Change back to previous slot
        mc.player.inventory.selectedSlot = prevSelectedSlot;
    });

    @EventHandler
    private final Listener<ClipAtLedgeEvent> onClipAtLedge = new Listener<>(event -> {
        if (mc.player.input.sneaking) {
            event.setClip(false);
            return;
        }

        if (safeWalk.get()) event.setClip(true);
    });

    private boolean findBlock() {
        if (mc.player.inventory.getStack(slot).isEmpty()) {
            slot = findSlot(blockState);
            if (slot == -1) {
                mc.player.inventory.selectedSlot = prevSelectedSlot;
                if (selfToggle.get()) this.toggle();
                return false;
            }
        }

        return true;
    }

    private boolean isSolid(BlockState state) {
        return state.getOutlineShape(mc.world, setPos(0, -1, 0)) != VoxelShapes.empty();
    }

    private BlockPos setPos(int x, int y, int z) {
        blockPos.set(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (x != 0) blockPos.setX(blockPos.getX() + x);
        if (y != 0) blockPos.setY(blockPos.getY() + y);
        if (z != 0) blockPos.setZ(blockPos.getZ() + z);
        return blockPos;
    }

    private int findSlot(BlockState blockState) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStack(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;

            if (blackList.get().contains(Block.getBlockFromItem(stack.getItem()))) continue;

            // Filter out non solid blocks
            Block block = ((BlockItem) stack.getItem()).getBlock();
            slotBlockState = block.getDefaultState();
            if (!Block.isShapeFullCube(slotBlockState.getCollisionShape(mc.world, setPos(0, -1, 0)))) continue;

            // Filter out blocks that would fall
            if (block instanceof FallingBlock && FallingBlock.canFallThrough(blockState)) continue;

            slot = i;
            break;
        }

        ItemStack handStack = mc.player.getMainHandStack();
        if (handStack.isEmpty() || !(handStack.getItem() instanceof BlockItem)) return slot;

        if (blackList.get().contains(Block.getBlockFromItem(handStack.getItem()))) return slot;

        // Filter out non solid blocks
        Block block = ((BlockItem) handStack.getItem()).getBlock();
        slotBlockState = block.getDefaultState();
        if (!Block.isShapeFullCube(slotBlockState.getCollisionShape(mc.world, setPos(0, -1, 0)))) return slot;

        // Filter out blocks that would fall
        if (block instanceof FallingBlock && FallingBlock.canFallThrough(blockState)) return slot;
        slot = mc.player.inventory.selectedSlot;

        return slot;
    }

    public boolean hasSafeWalk() {
        return safeWalk.get();
    }
}