/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

//Created by squidoodly 03/06/2020
//Updated by squidoodly 19/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.DamageCalcUtils;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.stream.Collectors;

public class BedAura extends ToggleModule {
    public enum Mode{
        safe,
        suicide
    }

    public BedAura(){
        super(Category.Combat, "bed-aura", "Automatically places and blows up beds in the nether");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The distance in a single direction the beds get placed.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The distance in a single direction the beds get broken.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("The way beds are placed")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Toggles this in the overworld.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Mode> clickMode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("break-mode")
            .description("The way the beds are broken")
            .defaultValue(Mode.safe)
            .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to bed automatically")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches back to the previous slot after auto switching.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoMove = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-move")
            .description("Moves beds into your last hotbar slot.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay between placements.")
            .defaultValue(2)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> smartDelay = sgGeneral.add(new BoolSetting.Builder()
            .name("smart-delay")
            .description("Reduces crystal consumption when doing large amounts of damage.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> healthDifference = sgGeneral.add(new DoubleSetting.Builder()
            .name("damage-increase")
            .description("The damage increase for smart delay to work.")
            .defaultValue(5)
            .min(0)
            .max(20)
            .build()
    );

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Places beds in the air if they do more damage.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage the beds will place")
            .defaultValue(5.5)
            .build()
    );

    private final Setting<Double> maxDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("max-damage")
            .description("The maximum self-damage allowed")
            .defaultValue(3)
            .build()
    );

    private final Setting<Double> minHealth = sgPlace.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health you have to be for it to place")
            .defaultValue(15)
            .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
            .name("place")
            .description("Allow it to place beds")
            .defaultValue(true)
            .build()
    );

    private int delayLeft = delay.get();
    private Vec3d bestBlock;
    private double bestDamage;
    private BlockPos playerPos;
    private double currentDamage;
    private BlockPos bestBlockPos;
    private BlockPos pos;
    private Vec3d vecPos;
    private BlockPos posUp;
    private float preYaw;
    private double lastDamage = 0;
    private int direction = 0;
    int preSlot = -1;

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        delayLeft --;
        preSlot = -1;
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= minHealth.get() && mode.get() != Mode.suicide) return;
        try {
            for (BlockEntity entity : mc.world.blockEntities) {
                if (entity instanceof BedBlockEntity && Utils.distance(entity.getPos().getX(), entity.getPos().getY(), entity.getPos().getZ(), mc.player.getX(), mc.player.getY(), mc.player.getZ()) <= breakRange.get()) {
                    currentDamage = DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(entity.getPos()));
                    if (currentDamage < maxDamage.get()
                            || (mc.player.getHealth() + mc.player.getAbsorptionAmount() - currentDamage) < minHealth.get() || clickMode.get().equals(Mode.suicide)) {
                        mc.player.setSneaking(false);
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, entity.getPos(), false));
                    }

                }
            }
        } catch (ConcurrentModificationException ignored) {
            return;
        }
        if (selfToggle.get() && mc.world.getDimension().isBedWorking()) {
            Chat.warning(this, "You are in the overworld. Disabling!");
            this.toggle();
            return;
        }
        if ((mc.player.getHealth() + mc.player.getAbsorptionAmount()) <= minHealth.get() && mode.get() != Mode.suicide)
            return;
        if (place.get() && (!(mc.player.getMainHandStack().getItem() instanceof BedItem)
                && !(mc.player.getOffHandStack().getItem() instanceof BedItem)) && !autoSwitch.get() && !autoMove.get()) return;
        if (place.get()) {
            boolean doMove = true;
            if (!(mc.player.getMainHandStack().getItem() instanceof BedItem)
                    && !(mc.player.getOffHandStack().getItem() instanceof BedItem)){
                if (autoMove.get()){
                    for (int i = 0; i < 9; i++) {
                        if (mc.player.inventory.getStack(i).getItem() instanceof BedItem) {
                            doMove = false;
                            break;
                        }
                    }
                    if (doMove){
                        int slot = -1;
                        for (int i = 0; i < mc.player.inventory.main.size(); i++){
                            ItemStack itemStack = mc.player.inventory.main.get(i);
                            if (itemStack.getItem() instanceof BedItem){
                                slot = i;
                            }
                        }
                        InvUtils.clickSlot(InvUtils.invIndexToSlotId(8), 0, SlotActionType.PICKUP);
                        InvUtils.clickSlot(InvUtils.invIndexToSlotId(slot), 0, SlotActionType.PICKUP);
                        InvUtils.clickSlot(InvUtils.invIndexToSlotId(8), 0, SlotActionType.PICKUP);
                    }
                }
                if (autoSwitch.get()){
                    for (int i = 0; i < 9; i++) {
                        if (mc.player.inventory.getStack(i).getItem() instanceof BedItem) {
                            preSlot = mc.player.inventory.selectedSlot;
                            mc.player.inventory.selectedSlot = i;
                            break;
                        }
                    }
                }

            }
            if (!(mc.player.getMainHandStack().getItem() instanceof BedItem)
                    && !(mc.player.getOffHandStack().getItem() instanceof BedItem)){
                return;
            }
            Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream()
                    .filter(FriendManager.INSTANCE::attack)
                    .filter(entityPlayer -> !entityPlayer.getDisplayName().equals(mc.player.getDisplayName()))
                    .filter(entityPlayer -> mc.player.distanceTo(entityPlayer) <= 10)
                    .filter(entityPlayer -> !entityPlayer.isCreative() && !entityPlayer.isSpectator())
                    .collect(Collectors.toList()).iterator();

            AbstractClientPlayerEntity target;
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
            if (!smartDelay.get() && delayLeft > 0) return;
            findValidBlocks(target);
            if (bestBlock != null && bestDamage >= minDamage.get()) {
                if (!smartDelay.get()) {
                    delayLeft = delay.get();
                    placeBlock();
                }else if (smartDelay.get() && (delayLeft <= 0 || bestDamage - lastDamage > healthDifference.get())) {
                    lastDamage = bestDamage;
                    placeBlock();
                    if (delayLeft <= 0) delayLeft = 10;
                }
            }
        }
    });

    private void placeBlock(){
        bestBlockPos = new BlockPos(bestBlock.x, bestBlock.y, bestBlock.z);
        Hand hand = Hand.MAIN_HAND;
        if (!(mc.player.getMainHandStack().getItem() instanceof BedItem) && mc.player.getOffHandStack().getItem() instanceof BedItem) {
            hand = Hand.OFF_HAND;
        }
        if (direction == 0) {
            preYaw = mc.player.yaw;
            mc.player.yaw = -90f;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(-90f, mc.player.pitch, mc.player.isOnGround()));
            mc.player.yaw = preYaw;
        } else if (direction == 1) {
            preYaw = mc.player.yaw;
            mc.player.yaw = 179f;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(179f, mc.player.pitch, mc.player.isOnGround()));
            mc.player.yaw = preYaw;
        } else if (direction == 2) {
            preYaw = mc.player.yaw;
            mc.player.yaw = 1f;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(1f, mc.player.pitch, mc.player.isOnGround()));
            mc.player.yaw = preYaw;
        } else if (direction == 3) {
            preYaw = mc.player.yaw;
            mc.player.yaw = 90f;
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(90f, mc.player.pitch, mc.player.isOnGround()));
            mc.player.yaw = preYaw;
        }
        lastDamage = bestDamage;
        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(bestBlock, Direction.UP, bestBlockPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (preSlot != -1 && mc.player.inventory.selectedSlot != preSlot && switchBack.get()) {
            mc.player.inventory.selectedSlot = preSlot;
        }
    }

    private void findValidBlocks(PlayerEntity target){
        bestBlock = null;
        bestDamage = 0;
        playerPos = mc.player.getBlockPos();
        for(double i = playerPos.getX() - placeRange.get(); i < playerPos.getX() + placeRange.get(); i++){
            for(double j = playerPos.getZ() - placeRange.get(); j < playerPos.getZ() + placeRange.get(); j++){
                for(double k = playerPos.getY() - 3; k < playerPos.getY() + 3; k++) {
                    pos = new BlockPos(i, k, j);
                    vecPos = new Vec3d(i, k, j);
                    posUp = pos.add(0, 1, 0);
                    if ((mc.world.getBlockState(posUp).getMaterial().isReplaceable())
                            && mc.world.getOtherEntities(null, new Box(posUp.getX(), posUp.getY(), posUp.getZ(), posUp.getX() + 1.0D, posUp.getY() + 1.0D, posUp.getZ() + 1.0D)).isEmpty()
                            && (mc.world.getBlockState(new BlockPos(posUp).add(1, 0, 0)).getMaterial().isReplaceable() || mc.world.getBlockState(posUp.add(-1, 0, 0)).getMaterial().isReplaceable()
                            || mc.world.getBlockState(posUp.add(0, 0, 1)).getMaterial().isReplaceable() || mc.world.getBlockState(posUp.add(0, 0, -1)).getMaterial().isReplaceable())) {
                        if (airPlace.get()) {
                            if (bestBlock == null) {
                                bestBlock = vecPos;
                                bestDamage = DamageCalcUtils.bedDamage(target, bestBlock.add(0.5, 1.5, 0.5));
                            }
                            if (bestDamage < DamageCalcUtils.bedDamage(target, vecPos.add(0.5, 1.5, 0.5))
                                    && (DamageCalcUtils.bedDamage(mc.player, vecPos.add(0.5, 1.5, 0.5)) < minDamage.get() || mode.get() == Mode.suicide)) {
                                bestBlock = vecPos;
                                bestDamage = DamageCalcUtils.bedDamage(target, bestBlock.add(0.5, 1.5, 0.5));
                            }
                        } else if (!airPlace.get() && !mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                            if (bestBlock == null) {
                                bestBlock = vecPos;
                                bestDamage = DamageCalcUtils.bedDamage(target, bestBlock.add(0.5, 1.5, 0.5));
                            }
                            if (bestDamage < DamageCalcUtils.bedDamage(target, vecPos.add(0.5, 1.5, 0.5))
                                    && (DamageCalcUtils.bedDamage(mc.player, vecPos.add(0.5, 1.5, 0.5)) < minDamage.get() || mode.get() == Mode.suicide)) {
                                bestBlock = vecPos;
                                bestDamage = DamageCalcUtils.bedDamage(target, bestBlock.add(0.5, 1.5, 0.5));
                            }
                        }
                    }
                }
            }
        }
        if (bestBlock == null) return;
        double north = -1;
        double east = -1;
        double south = -1;
        double west = -1;
        bestBlockPos = new BlockPos(bestBlock.x, bestBlock.y, bestBlock.z);
        if (mc.world.getBlockState(bestBlockPos.add(1, 1, 0)).getMaterial().isReplaceable()) {
            east = DamageCalcUtils.bedDamage(target, bestBlock.add(1.5, 1.5, 0.5));
        }
        if (mc.world.getBlockState(bestBlockPos.add(-1, 1, 0)).getMaterial().isReplaceable()) {
            west = DamageCalcUtils.bedDamage(target, bestBlock.add(-1.5, 1.5, 0.5));
        }
        if (mc.world.getBlockState(bestBlockPos.add(0, 1, 1)).getMaterial().isReplaceable()) {
            south = DamageCalcUtils.bedDamage(target, bestBlock.add(0.5, 1.5, 1.5));
        }
        if (mc.world.getBlockState(bestBlockPos.add(0, 1, -1)).getMaterial().isReplaceable()) {
            north = DamageCalcUtils.bedDamage(target, bestBlock.add(0.5, 1.5, -1.5));
        }

        if ((east > north) && (east > south) && (east > west)) {
            direction = 0;
        } else if ((east < north) && (north > south) && (north > west)) {
            direction = 1;
        } else if ((south > north) && (east < south) && (south > west)) {
            direction = 2;
        } else if ((west > north) && (west > south) && (east < west)) {
            direction = 3;
        }
        bestDamage = Math.max(bestDamage, Math.max(north, Math.max(east, Math.max(south, west))));
    }
}
