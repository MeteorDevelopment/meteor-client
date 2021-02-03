/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

// Created by squidoodly 03/06/2020
// Updated by squidoodly 19/06/2020

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.FakePlayerUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.RotationUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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
            .defaultValue(14)
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

    private final Setting<Boolean> autoSwitch = sgMisc.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to a bed automatically.")
            .defaultValue(true)
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

    private final Setting<Boolean> switchBack = sgMisc.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches back to the previous slot after auto switching.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoMove = sgMisc.add(new BoolSetting.Builder()
            .name("auto-move")
            .description("Moves beds into your last hotbar slot.")
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

    private Vec3d bestBlock;
    private BlockPos bestBlockPos;
    private int direction = 0;
    int preSlot = -1;
    boolean bypassCheck = false;
    private AbstractClientPlayerEntity target;
    private int breakDelayLeft;
    private int placeDelayLeft;
    int stage = 1;

    @Override
    public void onActivate() {
        placeDelayLeft = placeDelay.get();
        breakDelayLeft = breakDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (target == null || target.isDead() || mc.player.distanceTo(target) > range.get()) return;
        preSlot = -1;
        if (mc.player.getHealth() <= minHealth.get()) return;
        if (mc.world.getDimension().isBedWorking()) {
            ChatUtils.moduleError(this, "You are in the Overworld... disabling!");
            this.toggle();
            return;
        }
        if ((!(mc.player.getMainHandStack().getItem() instanceof BedItem)
                && !(mc.player.getOffHandStack().getItem() instanceof BedItem)) && !autoSwitch.get() && !autoMove.get()) return;
        if (place.get()) {
            target = null;
            for (FakePlayerEntity player : FakePlayerUtils.getPlayers().keySet()){
                if (target == null) target = player;

                else if (mc.player.distanceTo(player) < mc.player.distanceTo(target)){
                    target = player;
                }
            }
            if (target == null) {
                Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream()
                        .filter(Friends.get()::attack)
                        .filter(entityPlayer -> !entityPlayer.getDisplayName().equals(mc.player.getDisplayName()))
                        .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) <= 10)
                        .filter(entityPlayer -> !entityPlayer.isCreative() && !entityPlayer.isSpectator())
                        .collect(Collectors.toList()).iterator();

                if (validEntities.hasNext()) {
                    target = validEntities.next();
                } else {
                    return;
                }
                for (AbstractClientPlayerEntity i = null; validEntities.hasNext(); i = validEntities.next()) {
                    if (i == null) continue;
                    if (mc.player.distanceTo(i) < mc.player.distanceTo(target)) {
                        target = i;
                    }
                }
            }
            if (target == null) return;
            switch (stage) {
                case 1:
                    placeDelayLeft--;
                    if (placeDelayLeft <= 0) {
                        findFacePlace(target);
                        if (bestBlock != null || (bypassCheck)) {
                            bypassCheck = false;
                            if (autoMove.get()) doAutoMove();
                            placeBed();
                            stage = 2;
                        }
                    }
                    break;
                case 2:
                    breakDelayLeft--;
                    if (breakDelayLeft <= 0) {
                        breakDelayLeft = breakDelay.get();
                        placeDelayLeft = placeDelay.get();
                        breakBed();
                        stage = 1;
                    }
                    break;
            }
        } else {
            breakBed();
        }
    }

    private void placeBed(){
        assert mc.player != null;
        assert mc.interactionManager != null;
        if (bedSlot() == -1) return;
        int preSlot = mc.player.inventory.selectedSlot;
        bestBlockPos = new BlockPos(bestBlock.x, bestBlock.y, bestBlock.z);
        Hand hand = Hand.MAIN_HAND;
        if (!(mc.player.getMainHandStack().getItem() instanceof BedItem) && mc.player.getOffHandStack().getItem() instanceof BedItem) {
            hand = Hand.OFF_HAND;
        }
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
        if (autoSwitch.get()) mc.player.inventory.selectedSlot = bedSlot();
        mc.player.setSneaking(false);
        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(mc.player.getPos(), Direction.UP, bestBlockPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (switchBack.get()) mc.player.inventory.selectedSlot = preSlot;
    }

    private void breakBed() {
        try {
            for (BlockEntity entity : mc.world.blockEntities) {
                if (entity instanceof BedBlockEntity && Utils.distance(entity.getPos().getX(), entity.getPos().getY(), entity.getPos().getZ(), mc.player.getX(), mc.player.getY(), mc.player.getZ()) <= range.get()) {
                    mc.player.setSneaking(false);
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, entity.getPos(), false));
                }

            }
        } catch (ConcurrentModificationException ignored) {
            return;
        }
    }

    private void findFacePlace(PlayerEntity target) {
        assert mc.world != null;
        assert mc.player != null;
        if (mc.player.distanceTo(target) < range.get() && mc.world.isAir(target.getBlockPos().add(0, 1, 0))) {
            if (isValidHalf(target.getBlockPos().add(1, 0, 0))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 3;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(-1, 0, 0))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 0;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 0, 1))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 1.5);
                direction = 1;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 0, -1))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() - 0.5);
                direction = 2;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(1, 1, 0))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 3;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(-1, 1, 0))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 0.5);
                direction = 0;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 1, 1))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() + 1.5);
                direction = 1;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 1, -1))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 1, target.getBlockPos().getZ() - 0.5);
                direction = 2;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(1, 2, 0))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 1.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 0.5);
                direction = 3;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(-1, 2, 0))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() - 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 0.5);
                direction = 0;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 2, 1))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() + 1.5);
                direction = 1;
                bypassCheck = true;
            } else if (isValidHalf(target.getBlockPos().add(0, 2, -1))) {
                bestBlock = new Vec3d(target.getBlockPos().getX() + 0.5, target.getBlockPos().getY() + 2, target.getBlockPos().getZ() - 0.5);
                direction = 2;
                bypassCheck = true;
            }
        }
    }

    private boolean isValidHalf(BlockPos pos) {
        assert mc.world != null;
        return (!mc.world.isAir(pos)) && mc.world.isAir(pos.up());
    }

    private int bedSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();
            if (item instanceof BedItem) {
                slot = i;
                break;
            }
        }
        return slot;
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

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }
}
