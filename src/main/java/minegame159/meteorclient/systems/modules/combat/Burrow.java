/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.meteor.KeyEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.world.Timer;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.input.KeyAction;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.block.BlockState;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * @author seasnail8169
 */
public class Burrow extends Module {

    public enum Block {
        EChest,
        Obsidian
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Block> block = sgGeneral.add(new EnumSetting.Builder<Block>()
            .name("block-to-use")
            .description("The block to use for Burrow.")
            .defaultValue(Block.EChest)
            .build()
    );

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant")
            .description("Jumps with packets rather than vanilla jump.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> automatic = sgGeneral.add(new BoolSetting.Builder()
            .name("automatic")
            .description("Automatically burrows on activate rather than waiting for jump.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> triggerHeight = sgGeneral.add(new DoubleSetting.Builder()
            .name("trigger-height")
            .description("How high you have to jump before a rubberband is triggered.")
            .defaultValue(1.12)
            .min(0.01)
            .sliderMax(1.4)
            .build()
    );

    private final Setting<Double> rubberbandHeight = sgGeneral.add(new DoubleSetting.Builder()
            .name("rubberband-height")
            .description("How far to attempt to cause rubberband.")
            .defaultValue(12)
            .sliderMin(-30)
            .sliderMax(30)
            .build()
    );

    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
            .name("timer")
            .description("Timer override.")
            .defaultValue(1.00)
            .min(0.01)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder()
            .name("only-in-holes")
            .description("Stops you from burrowing when not in a hole.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
            .name("center")
            .description("Centers you to the middle of the block before burrowing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Faces the block you place server-side.")
            .defaultValue(true)
            .build()
    );

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    private int prevSlot;
    private int slot;

    private boolean shouldBurrow;

    public Burrow() {
        super(Categories.Combat, "Burrow", "Attempts to clip you into a block.");
    }

    @Override
    public void onActivate() {
        if (!mc.world.getBlockState(mc.player.getBlockPos()).getMaterial().isReplaceable()) {
            ChatUtils.moduleError(this, "Already burrowed, disabling.");
            toggle();
            return;
        }

        if (!PlayerUtils.isInHole(false) && onlyInHole.get()) {
            ChatUtils.moduleError(this, "Not in a hole, disabling.");
            toggle();
            return;
        }

        if (!checkHead()) {
            ChatUtils.moduleError(this, "Not enough headroom to burrow, disabling.");
            toggle();
            return;
        }

        if (!checkInventory()) {
            ChatUtils.moduleError(this, "No burrow block found, disabling.");
            toggle();
            return;
        }

        blockPos.set(mc.player.getBlockPos());

        Modules.get().get(Timer.class).setOverride(this.timer.get());

        shouldBurrow = false;

        if (automatic.get()) {
            if (instant.get()) shouldBurrow = true;
            else mc.player.jump();
        } else {
            ChatUtils.moduleInfo(this, "Waiting for manual jump.");
        }
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!instant.get()) shouldBurrow = mc.player.getY() > blockPos.getY() + triggerHeight.get();
        if (!shouldBurrow && instant.get()) blockPos.set(mc.player.getBlockPos());

        if (shouldBurrow) {
            if (!checkInventory()) return;
            if (!mc.player.isOnGround()) {
                toggle();
                return;
            }

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(mc.player.getBlockPos()), Rotations.getPitch(mc.player.getBlockPos()), 50, this::burrow);
            else burrow();

            toggle();
        }
    }

    private void burrow() {
        if (center.get()) PlayerUtils.centerPlayer();

        if (instant.get()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), true));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), true));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), true));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), true));
        }

        mc.player.inventory.selectedSlot = slot;

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(blockPos), Direction.UP, blockPos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        mc.player.inventory.selectedSlot = prevSlot;

        if (instant.get()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionOnly(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ(), false));
        } else {
            mc.player.setVelocity(mc.player.getVelocity().add(0, rubberbandHeight.get(), 0));
        }
    }


    @EventHandler
    private void onKey(KeyEvent event) {
        if (instant.get() && !shouldBurrow) {
            if (event.action == KeyAction.Press && mc.options.keyJump.matchesKey(event.key, 0))
            shouldBurrow = true;
            blockPos.set(mc.player.getBlockPos());
        }
    }

    //Wala man
    private boolean checkHead() {
        BlockState blockState1 = mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        BlockState blockState2 = mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState3 = mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState4 = mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        boolean air1 = blockState1.getMaterial().isReplaceable();
        boolean air2 = blockState2.getMaterial().isReplaceable();
        boolean air3 = blockState3.getMaterial().isReplaceable();
        boolean air4 = blockState4.getMaterial().isReplaceable();
        return air1 & air2 & air3 & air4;
    }

    private boolean checkInventory() {
        prevSlot = mc.player.inventory.selectedSlot;
        switch (block.get()) {
            case Obsidian:  slot = InvUtils.findItemInHotbar(Items.OBSIDIAN); break;
            case EChest:    slot = InvUtils.findItemInHotbar(Items.ENDER_CHEST); break;
        }
        return slot != -1;
    }
}