/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.game.OpenScreenEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.Friends;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.entity.FakePlayerEntity;
import minegame159.meteorclient.utils.entity.FakePlayerUtils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

// Created by Eureka

public class AutoAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");

    // General

    private final Setting<Boolean> toggleOnBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("toggle-on-break")
            .description("Toggles when the target's helmet slot is empty.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position anvils/pressure plates/buttons are placed.")
            .defaultValue(true)
            .build()
    );

    // Place

    private final Setting<Double> range = sgPlace.add(new DoubleSetting.Builder()
            .name("range")
            .description("How far away the target can be to be affected.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    private final Setting<Integer> delay = sgPlace.add(new IntSetting.Builder()
            .name("delay")
            .description("The delay in between anvil placements.")
            .min(0)
            .defaultValue(0)
            .sliderMax(50)
            .build()
    );

    private final Setting<Integer> height = sgPlace.add(new IntSetting.Builder()
            .name("height")
            .description("The height at which to place the anvils.")
            .defaultValue(5)
            .min(0)
            .max(10)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> placeButton = sgPlace.add(new BoolSetting.Builder()
            .name("place-at-feet")
            .description("Automatically places a button or pressure plate at the targets feet to break the anvils.")
            .defaultValue(true)
            .build()
    );

    public AutoAnvil() {
        super(Category.Combat, "auto-anvil", "Automatically places anvils above players to destroy helmets.");
    }

    private PlayerEntity target;
    private int timer;

    @Override
    public void onActivate() {
        timer = 0;
        target = null;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof AnvilScreen) mc.player.closeScreen();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isActive() && toggleOnBreak.get() && target != null && target.inventory.getArmorStack(3).isEmpty()) {
            ChatUtils.moduleError(this, "Target head slot is empty... disabling.");
            toggle();
            return;
        }

        if (target != null && (mc.player.distanceTo(target) > range.get() || !target.isAlive())) target = null;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !Friends.get().attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

            if (target == null) target = player;
            else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) target = player;
        }

        if (target == null) {
            for (FakePlayerEntity player : FakePlayerUtils.getPlayers().keySet()) {
                if (!Friends.get().attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

                if (target == null) target = player;
                else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) target = player;
            }
        }

        if (timer >= delay.get() && target != null) {
            timer = 0;

            int slot = getAnvilSlot();
            if (slot == -1) return;

            if (placeButton.get()) {
                int slot2 = getFloorSlot();
                BlockPos blockPos = target.getBlockPos();

                BlockUtils.place(blockPos, Hand.MAIN_HAND, slot2, rotate.get(), 0);
            }

            BlockPos blockPos = target.getBlockPos().up().add(0, height.get(), 0);
            BlockUtils.place(blockPos, Hand.MAIN_HAND, slot, rotate.get(), 0);
        } else timer++;
    }

    public int getFloorSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();
            Block block = Block.getBlockFromItem(item);

            if (block instanceof AbstractPressurePlateBlock || block instanceof AbstractButtonBlock) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private int getAnvilSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();
            Block block = Block.getBlockFromItem(item);

            if (block instanceof AnvilBlock) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    @Override
    public String getInfoString() {
        if (target != null && target instanceof PlayerEntity) return target.getEntityName();
        if (target != null) return target.getType().getName().getString();
        return null;
    }
}
