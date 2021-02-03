/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

// Created by squidoodly 03/06/2020
// Updated by squidoodly 19/06/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.RotationUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

@InvUtils.Priority(priority = 0)
public class BedAura extends Module {
    public enum Mode{
        Safe,
        Suicide
    }

    public BedAura(){
        super(Category.Combat, "bed-aura", "Automatically places and explodes beds in the Nether and End.");
    }

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    // Place

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("Allows Bed Aura to place beds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The tick delay for placing beds.")
            .defaultValue(11)
            .min(0)
            .sliderMax(20)
            .build()
    );

    // Break

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
            .name("break-delay")
            .description("The tick delay for breaking beds.")
            .defaultValue(1)
            .min(0)
            .sliderMax(20)
            .build()
    );

    // Misc

    private final Setting<Double> range = sgMisc.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range for beds to placed.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgMisc.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to a bed automatically.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> autoMove = sgMisc.add(new BoolSetting.Builder()
            .name("auto-move")
            .description("Moves beds into a hotbar slot.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> autoMoveSlot = sgMisc.add(new IntSetting.Builder()
            .name("auto-move-slot")
            .description("The slot Auto Move moves beds to.")
            .defaultValue(8)
            .min(1)
            .max(8)
            .build()
    );

    private final Setting<Double> minHealth = sgMisc.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health required for Bed Aura to work.")
            .defaultValue(4)
            .min(0)
            .sliderMax(36)
            .max(36)
            .build()
    );

    private final Setting<SortPriority> priority = sgMisc.add(new EnumSetting.Builder<SortPriority>()
            .name("priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    private int direction = 0;
    boolean bypassCheck = false;
    private PlayerEntity target;
    private int breakDelayLeft;
    private int placeDelayLeft;
    int stage = 1;

    @Override
    public void onActivate() {
        placeDelayLeft = placeDelay.get();
        breakDelayLeft = breakDelay.get();
        target = null;
    }

    @Override
    public void onDeactivate() {
        target = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world.getDimension().isBedWorking()) {
            ChatUtils.moduleError(this, "You are in the Overworld... disabling!");
            toggle();
            return;
        }

        if (mc.player.getHealth() <= minHealth.get()) return;

        if (target == null
                || target.isDead()
                || mc.player.distanceTo(target) > range.get()) {
            target = findTarget();
        }

        if (target == null) return;

        if (place.get() && hasBeds()) {
            switch (stage) {
                case 1:
                    if (placeDelayLeft > 0) placeDelayLeft--;

                    else {
                        BlockPos place = findFacePlace(target);

                        if (place != null || (bypassCheck)) {
                            bypassCheck = false;
                            if (autoMove.get()) doAutoMove();
                            placeBed(place);
                            stage = 2;
                        }

                    }

                    break;
                case 2:
                    if (breakDelayLeft > 0) breakDelayLeft--;

                    else {
                        breakDelayLeft = breakDelay.get();
                        placeDelayLeft = placeDelay.get();
                        breakBed();
                        stage = 1;
                    }
                    break;
            }
        } else {
            if (breakDelayLeft > 0) breakDelayLeft--;

            else {
                breakDelayLeft = breakDelay.get();
                breakBed();
                stage = 1;
            }
        }
    }

    private void placeBed(BlockPos pos) {
        if (pos == null) return;

        int slot = InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (slot == -1) return;

        if (autoSwitch.get()) mc.player.inventory.selectedSlot = slot;

        Hand hand = InvUtils.getHand(itemStack -> itemStack.getItem() instanceof BedItem);

        if (hand == null) return;

        mc.player.setSneaking(false);

        switch (direction) {
            case 0:
                RotationUtils.packetRotate(-90, mc.player.pitch);
                break;
            case 1:
                RotationUtils.packetRotate(179, mc.player.pitch);
                break;
            case 2:
                RotationUtils.packetRotate(1, mc.player.pitch);
                break;
            case 3:
                RotationUtils.packetRotate(90, mc.player.pitch);
                break;
        }

        BlockUtils.place(pos, hand, slot, false, 100);
    }

    private void breakBed() {
        try {
            for (BlockEntity entity : mc.world.blockEntities) {
                if (entity instanceof BedBlockEntity && Utils.distance(entity.getPos().getX(), entity.getPos().getY(), entity.getPos().getZ(), mc.player.getX(), mc.player.getY(), mc.player.getZ()) <= range.get()) {
                    mc.player.setSneaking(false);
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, entity.getPos(), false));
                }

            }
        } catch (ConcurrentModificationException ignored) {}
    }

    private BlockPos findFacePlace(PlayerEntity target) {

        BlockPos bestBlock = null;

        if (mc.player.distanceTo(target) < range.get() && mc.world.isAir(target.getBlockPos().add(0, 1, 0))) {
            if (isValidHalf(target.getBlockPos().add(1, 0, 0))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 3;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(-1, 0, 0))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 0;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 0, 1))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 1.5);
                direction = 1;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 0, -1))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() - 0.5);
                direction = 2;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(1, 1, 0))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 3;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(-1, 1, 0))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 0;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 1, 1))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 1.5);
                direction = 1;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 1, -1))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() - 0.5);
                direction = 2;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(1, 2, 0))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 0.5);
                direction = 3;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(-1, 2, 0))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 0.5);
                direction = 0;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 2, 1))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 1.5);
                direction = 1;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 2, -1))) {
                bestBlock = new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() - 0.5);
                direction = 2;
                bypassCheck = true;
            }
        }
        return bestBlock;
    }

    private boolean isValidHalf(BlockPos pos) {
        return (!mc.world.isAir(pos)) && mc.world.isAir(pos.up());
    }

    private void doAutoMove() {
        boolean doMove = true;
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStack(i).getItem() instanceof BedItem) {
                doMove = false;
                break;
            }
        }
        if (doMove) {
            int slot = -1;
            for (int i = 0; i < mc.player.inventory.main.size(); i++) {
                ItemStack itemStack = mc.player.inventory.main.get(i);
                if (itemStack.getItem() instanceof BedItem) {
                    slot = i;
                }
            }
            List<Integer> slots = new ArrayList<>();
            slots.add(InvUtils.invIndexToSlotId(autoMoveSlot.get()));
            slots.add(InvUtils.invIndexToSlotId(slot));
            slots.add(InvUtils.invIndexToSlotId(autoMoveSlot.get()));
            InvUtils.addSlots(slots, this.getClass());
        }
    }

    private PlayerEntity findTarget() {
        return (PlayerEntity) EntityUtils.get(entity -> {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) return false;
            if (((PlayerEntity) entity).isDead() || ((PlayerEntity) entity).getHealth() <= 0) return false;
            if (mc.player.distanceTo(entity) > range.get()) return false;
            return !((PlayerEntity) entity).isCreative() && !entity.isSpectator();
        }, priority.get());
    }

    private boolean hasBeds() {
        for (int i = 0; i < mc.player.inventory.size(); i++){
            if (mc.player.inventory.getStack(i).getItem() instanceof BedItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}