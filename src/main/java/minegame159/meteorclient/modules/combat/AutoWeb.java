/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.player.FakePlayer;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class AutoWeb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The maximum distance to be able to place webs.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
            .name("doubles")
            .description("Places webs in the target's upper hitbox as well as the lower hitbox.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards the webs when placing.")
            .defaultValue(true)
            .build()
    );

    public AutoWeb() {
        super(Category.Combat, "auto-web", "Automatically places webs on other players.");
    }

    private PlayerEntity target = null;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        int slot = findSlot();
        if (slot == -1) return;

        if (target != null) {
            if (mc.player.distanceTo(target) > range.get() || !target.isAlive()) target = null;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !FriendManager.INSTANCE.attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

            if (target == null) {
                target = player;
            } else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) {
                target = player;
            }
        }

        if (target == null) {
            for (FakePlayerEntity fakeTarget : FakePlayer.players.keySet()) {
                if (fakeTarget.getHealth() <= 0 || !FriendManager.INSTANCE.attack(fakeTarget) || !fakeTarget.isAlive()) continue;

                if (target == null) {
                    target = fakeTarget;
                    continue;
                }

                if (mc.player.distanceTo(fakeTarget) < mc.player.distanceTo(target)) target = fakeTarget;
            }
        }

        if (target != null) {
            BlockPos targetPos = target.getBlockPos();

            if (PlayerUtils.canPlace(targetPos)) {
                BlockPos blockPos = targetPos;
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> PlayerUtils.placeBlock(blockPos, slot, Hand.MAIN_HAND));
                else PlayerUtils.placeBlock(blockPos, slot, Hand.MAIN_HAND);
            }

            targetPos = targetPos.add(0, 1, 0);
            if (PlayerUtils.canPlace(targetPos)) {
                BlockPos blockPos = targetPos;
                if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), () -> PlayerUtils.placeBlock(blockPos, slot, Hand.MAIN_HAND));
                else PlayerUtils.placeBlock(blockPos, slot, Hand.MAIN_HAND);
            }
        }
    }

    private int findSlot() {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (item == Items.COBWEB) {
                return i;
            }
        }

        return -1;
    }
}