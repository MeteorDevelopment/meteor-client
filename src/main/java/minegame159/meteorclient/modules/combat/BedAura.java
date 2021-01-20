/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

// Created by squidoodly 03/06/2020
// Updated by squidoodly 19/06/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.DamageCalcUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
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
    private final SettingGroup sgSwitch = settings.createGroup("Switch");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    // Place

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The delay between placements.")
            .defaultValue(2)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Mode> placeMode = sgPlace.add(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("How the beds get placed.")
            .defaultValue(Mode.Safe)
            .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The radius in which beds can be placed in.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Boolean> airPlace = sgPlace.add(new BoolSetting.Builder()
            .name("air-place")
            .description("Places beds in the air if they do more damage.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("Allows Bed Aura to place beds.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> minHealth = sgPlace.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health you have to be for Bed Aura to place.")
            .defaultValue(15)
            .build()
    );

    private final Setting<Double> minDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage the beds will place.")
            .defaultValue(5.5)
            .build()
    );

    private final Setting<Boolean> calcDamage = sgPlace.add(new BoolSetting.Builder()
            .name("damage-calc")
            .description("Whether to calculate damage (true) or just place on the head of the target (false).")
            .defaultValue(false)
            .build()
    );

    // Break

    private final Setting<Mode> breakMode = sgBreak.add(new EnumSetting.Builder<Mode>()
            .name("break-mode")
            .description("How beds are broken.")
            .defaultValue(Mode.Safe)
            .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The distance in a single direction the beds get broken.")
            .defaultValue(4)
            .min(0)
            .sliderMax(5)
            .build()
    );

    // Switch

    private final Setting<Boolean> autoSwitch = sgSwitch.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to a bed automatically.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> switchBack = sgSwitch.add(new BoolSetting.Builder()
            .name("switch-back")
            .description("Switches back to the previous slot after auto switching.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> autoMove = sgSwitch.add(new BoolSetting.Builder()
            .name("auto-move")
            .description("Moves beds into your last hotbar slot.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> autoMoveSlot = sgSwitch.add(new IntSetting.Builder()
            .name("auto-move-slot")
            .description("The slot Auto Move moves beds to.")
            .defaultValue(8)
            .min(0)
            .max(8)
            .build()
    );

    // Misc

    private final Setting<Boolean> selfToggle = sgMisc.add(new BoolSetting.Builder()
            .name("self-toggle")
            .description("Toggles Bed Aura in the Overworld.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> smartDelay = sgMisc.add(new BoolSetting.Builder()
            .name("smart-delay")
            .description("Reduces bed consumption when doing large amounts of damage.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> healthDifference = sgPlace.add(new DoubleSetting.Builder()
            .name("damage-increase")
            .description("The damage increase for smart delay to work.")
            .defaultValue(5)
            .min(0)
            .max(20)
            .build()
    );

    private final Setting<Double> maxDamage = sgPlace.add(new DoubleSetting.Builder()
            .name("max-damage")
            .description("The maximum self-damage allowed.")
            .defaultValue(3)
            .build()
    );

    private int delayLeft = placeDelay.get();
    private Vec3d bestBlock;
    private double bestDamage;
    private BlockPos bestBlockPos;
    private BlockPos pos;
    private Vec3d vecPos;
    private double lastDamage = 0;
    private int direction = 0;
    int preSlot = -1;
    boolean bypassCheck = false;
    private AbstractClientPlayerEntity target;

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        assert mc.player != null;
        assert mc.world != null;
        assert mc.interactionManager != null;
        delayLeft --;
        preSlot = -1;
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() <= minHealth.get() && placeMode.get() != Mode.Suicide) return;
        if (selfToggle.get() && mc.world.getDimension().isBedWorking()) {
            ChatUtils.moduleError(this, "You are in the Overworld... (highlight)disabling(default)!");
            this.toggle();
            return;
        }
        try {
            for (BlockEntity entity : mc.world.blockEntities) {
                if (entity instanceof BedBlockEntity && Utils.distance(entity.getPos().getX(), entity.getPos().getY(), entity.getPos().getZ(), mc.player.getX(), mc.player.getY(), mc.player.getZ()) <= breakRange.get()) {
                    double currentDamage = DamageCalcUtils.bedDamage(mc.player, Utils.vec3d(entity.getPos()));
                    if (currentDamage < maxDamage.get()
                            || (mc.player.getHealth() + mc.player.getAbsorptionAmount() - currentDamage) < minHealth.get() || breakMode.get().equals(Mode.Suicide)) {
                        mc.player.setSneaking(false);
                        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, entity.getPos(), false));
                    }

                }
            }
        } catch (ConcurrentModificationException ignored) {
            return;
        }
        if ((!(mc.player.getMainHandStack().getItem() instanceof BedItem)
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
                        List<Integer> slots = new ArrayList<>();
                        slots.add(InvUtils.invIndexToSlotId(autoMoveSlot.get()));
                        slots.add(InvUtils.invIndexToSlotId(slot));
                        slots.add(InvUtils.invIndexToSlotId(autoMoveSlot.get()));
                        InvUtils.addSlots(slots, this.getClass());
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
            target = null;
            for (Map.Entry<FakePlayerEntity, Integer> player : FakePlayer.players.entrySet()){
                if (target == null) {
                    target = player.getKey();
                } else if (mc.player.distanceTo(player.getKey()) < mc.player.distanceTo(target)){
                    target = player.getKey();
                }
            }
            if (target == null) {
                Iterator<AbstractClientPlayerEntity> validEntities = mc.world.getPlayers().stream()
                        .filter(FriendManager.INSTANCE::attack)
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
            if (!smartDelay.get() && delayLeft > 0) return;
            if (calcDamage.get()) {
                findValidBlocks(target);
            } else {
                findFacePlace(target);
            }
            if (bestBlock != null && (bestDamage >= minDamage.get() || bypassCheck)) {
                bypassCheck = false;
                if (!smartDelay.get()) {
                    delayLeft = placeDelay.get();
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
        assert mc.player != null;
        assert mc.interactionManager != null;
        bestBlockPos = new BlockPos(bestBlock.x, bestBlock.y, bestBlock.z);
        Hand hand = Hand.MAIN_HAND;
        if (!(mc.player.getMainHandStack().getItem() instanceof BedItem) && mc.player.getOffHandStack().getItem() instanceof BedItem) {
            hand = Hand.OFF_HAND;
        }
        if (direction == 0) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(-90f, mc.player.pitch, mc.player.isOnGround()));
        } else if (direction == 1) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(179f, mc.player.pitch, mc.player.isOnGround()));
        } else if (direction == 2) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(1f, mc.player.pitch, mc.player.isOnGround()));
        } else if (direction == 3) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(90f, mc.player.pitch, mc.player.isOnGround()));
        }
        lastDamage = bestDamage;
        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(mc.player.getPos(), Direction.UP, bestBlockPos, false));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (preSlot != -1 && mc.player.inventory.selectedSlot != preSlot && switchBack.get()) {
            mc.player.inventory.selectedSlot = preSlot;
        }
    }

    private void findValidBlocks(PlayerEntity target){
        assert mc.world != null;
        assert mc.player != null;
        bestBlock = null;
        bestDamage = 0;
        BlockPos playerPos = mc.player.getBlockPos();
        for(double i = playerPos.getX() - placeRange.get(); i < playerPos.getX() + placeRange.get(); i++){
            for(double j = playerPos.getZ() - placeRange.get(); j < playerPos.getZ() + placeRange.get(); j++){
                for(double k = playerPos.getY() - 3; k < playerPos.getY() + 3; k++) {
                    pos = new BlockPos(i, k, j);
                    vecPos = new Vec3d(Math.floor(i), Math.floor(k), Math.floor(j));
                    if (bestBlock == null) bestBlock = vecPos;
                    if (isValid(pos.up())) {
                        if (airPlace.get() || !mc.world.getBlockState(pos).getMaterial().isReplaceable()) {
                            if (bestDamage < getBestDamage(target, vecPos.add(0.5, 1.5, 0.5))
                                    && (DamageCalcUtils.bedDamage(mc.player, vecPos.add(0.5, 1.5, 0.5)) < minDamage.get() || placeMode.get() == Mode.Suicide)) {
                                bestBlock = vecPos;
                                bestDamage = getBestDamage(target, bestBlock.add(0.5, 1.5, 0.5));
                            }
                        }
                    }
                }
            }
        }
        if (bestDamage >= minDamage.get()) bestBlockPos = new BlockPos(bestBlock.x, bestBlock.y, bestBlock.z);
        else bestBlock = null;

    }

    private double getBestDamage(LivingEntity target, Vec3d bestBlock){
        double north, east, south, west, bestDamage;
        east = DamageCalcUtils.bedDamage(target, bestBlock.add(1, 0, 0));
        west = DamageCalcUtils.bedDamage(target, bestBlock.add(-1, 0, 0));
        south = DamageCalcUtils.bedDamage(target, bestBlock.add(0, 0, 1));
        north = DamageCalcUtils.bedDamage(target, bestBlock.add(0, 0, -1));
        bestDamage = DamageCalcUtils.bedDamage(target, bestBlock);

        if ((east > north) && (east > south) && (east > west)) {
            direction = 0;
        } else if ((east < north) && (north > south) && (north > west)) {
            direction = 1;
        } else if ((south > north) && (east < south) && (south > west)) {
            direction = 2;
        } else if ((west > north) && (west > south) && (east < west)) {
            direction = 3;
        }

        return Math.max(bestDamage, Math.max(north, Math.max(east, Math.max(south, west))));
    }

    private void findFacePlace(PlayerEntity target) {
        assert mc.world != null;
        assert mc.player != null;
        if (mc.player.distanceTo(target) < placeRange.get() && mc.world.isAir(target.getBlockPos().add(0, 1, 0))) {
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
        return (airPlace.get() || !mc.world.isAir(pos)) && mc.world.isAir(pos.up());
    }

    private boolean isValid(BlockPos posUp) {
        assert mc.world != null;
        return (mc.world.getBlockState(posUp).getMaterial().isReplaceable())
                && mc.world.getOtherEntities(null, new Box(posUp.getX(), posUp.getY(), posUp.getZ(), posUp.getX() + 1.0D, posUp.getY() + 1.0D, posUp.getZ() + 1.0D)).isEmpty()
                && (mc.world.getBlockState(new BlockPos(posUp).add(1, 0, 0)).getMaterial().isReplaceable() || mc.world.getBlockState(posUp.add(-1, 0, 0)).getMaterial().isReplaceable()
                || mc.world.getBlockState(posUp.add(0, 0, 1)).getMaterial().isReplaceable() || mc.world.getBlockState(posUp.add(0, 0, -1)).getMaterial().isReplaceable());
    }

    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity) return target.getEntityName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
}
