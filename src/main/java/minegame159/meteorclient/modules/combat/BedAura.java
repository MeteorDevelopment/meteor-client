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
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
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
    private final SettingGroup sgPause = settings.createGroup("Pause");
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

    // Pause

    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses while eating.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses while drinking potions.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses while mining blocks.")
            .defaultValue(false)
            .build()
    );

    // Misc

    private final Setting<Double> targetRange = sgMisc.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum range for players to be targeted.")
            .defaultValue(4)
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
            .description("Moves beds into a selected hotbar slot.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> autoMoveSlot = sgMisc.add(new IntSetting.Builder()
            .name("auto-move-slot")
            .description("The slot Auto Move moves beds to.")
            .defaultValue(9)
            .min(1)
            .sliderMin(1)
            .max(9)
            .sliderMax(9)
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

        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (EntityUtils.getTotalHealth(mc.player) <= minHealth.get()) return;

        if (EntityUtils.isInvalid(target, targetRange.get())) target = EntityUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (target == null) return;

        if (place.get() && InvUtils.findItemInAll(itemStack -> itemStack.getItem() instanceof BedItem) != -1) {
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
                Rotations.rotate(-90, mc.player.pitch, () -> BlockUtils.place(pos, hand, slot, false, 100));
                break;
            case 1:
                Rotations.rotate(179, mc.player.pitch, () -> BlockUtils.place(pos, hand, slot, false, 100));
                break;
            case 2:
                Rotations.rotate(1, mc.player.pitch, () -> BlockUtils.place(pos, hand, slot, false, 100));
                break;
            case 3:
                Rotations.rotate(90, mc.player.pitch, () -> BlockUtils.place(pos, hand, slot, false, 100));
                break;
        }
    }

    private void breakBed() {
        try {
            for (BlockEntity entity : mc.world.blockEntities) {
                if (entity instanceof BedBlockEntity && Utils.distance(entity.getPos().getX(), entity.getPos().getY(), entity.getPos().getZ(), mc.player.getX(), mc.player.getY(), mc.player.getZ()) <= targetRange.get() && (entity.getPos() != mc.player.getBlockPos())) {
                    mc.player.setSneaking(false);
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, entity.getPos(), false));
                }

            }
        } catch (ConcurrentModificationException ignored) {}
    }

    private BlockPos findFacePlace(PlayerEntity target) {
        if (mc.player.distanceTo(target) < targetRange.get() && mc.world.isAir(target.getBlockPos().add(0, 1, 0))) {
            if (isValidHalf(target.getBlockPos().add(1, 0, 0))) {
                direction = 3;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
            } else if (isValidHalf(target.getBlockPos().add(-1, 0, 0))) {
                direction = 0;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
            } else if (isValidHalf(target.getBlockPos().add(0, 0, 1))) {
                direction = 1;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 1.5);
            } else if (isValidHalf(target.getBlockPos().add(0, 0, -1))) {
                direction = 2;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() - 0.5);
            } else if (isValidHalf(target.getBlockPos().add(1, 1, 0))) {
                direction = 3;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
            } else if (isValidHalf(target.getBlockPos().add(-1, 1, 0))) {
                direction = 0;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
            } else if (isValidHalf(target.getBlockPos().add(0, 1, 1))) {
                direction = 1;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 1.5);
            } else if (isValidHalf(target.getBlockPos().add(0, 1, -1))) {
                direction = 2;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() - 0.5);
            } else if (isValidHalf(target.getBlockPos().add(1, 2, 0))) {
                direction = 3;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 0.5);
            } else if (isValidHalf(target.getBlockPos().add(-1, 2, 0))) {
                direction = 0;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 0.5);
            } else if (isValidHalf(target.getBlockPos().add(0, 2, 1))) {
                direction = 1;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 1.5);
            } else if (isValidHalf(target.getBlockPos().add(0, 2, -1))) {
                direction = 2;
                bypassCheck = true;
                return new BlockPos(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() - 0.5);
            }
        }
        return null;
    }

    private boolean isValidHalf(BlockPos pos) {
        return (!mc.world.isAir(pos)) && mc.world.isAir(pos.up());
    }

    private void doAutoMove() {
        if (InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof BedItem) == -1) {
            int slot = InvUtils.findItemInMain(itemStack -> itemStack.getItem() instanceof BedItem);
            List<Integer> slots = new ArrayList<>();
            slots.add(InvUtils.invIndexToSlotId(autoMoveSlot.get()-1));
            slots.add(InvUtils.invIndexToSlotId(slot));
            slots.add(InvUtils.invIndexToSlotId(autoMoveSlot.get()-1));
            InvUtils.addSlots(slots, this.getClass());
        }
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}