/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

// Created by Eureka

public class AutoAnvil extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("How far can the players be.")
            .defaultValue(4)
            .min(0)
            .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
            .name("height")
            .description("How high to place the anvils.")
            .defaultValue(5)
            .min(0)
            .max(10)
            .sliderMin(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> placeButton = sgGeneral.add(new BoolSetting.Builder()
            .name("place-button")
            .description("Auto places a button beneath the target.")
            .defaultValue(false)
            .build()
    );

    public AutoAnvil() {
        super(Category.Combat, "auto-anvil", "Automatically places anvils above players.");
    }

    private PlayerEntity target = null;

    @Override
    public void onDeactivate() {
        target = null;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        int anvilSlot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (item == Items.ANVIL || item == Items.CHIPPED_ANVIL || item == Items.DAMAGED_ANVIL) {
                anvilSlot = i;
                break;
            }
        }
        if (anvilSlot == -1) return;

        int buttonSlot = -1;
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.inventory.getStack(i).getItem();

            if (item == Items.ACACIA_BUTTON || item == Items.OAK_BUTTON || item == Items.STONE_BUTTON || item == Items.SPRUCE_BUTTON || item == Items.BIRCH_BUTTON || item == Items.BIRCH_BUTTON || item == Items.JUNGLE_BUTTON || item == Items.DARK_OAK_BUTTON || item == Items.CRIMSON_BUTTON || item == Items.WARPED_BUTTON || item == Items.POLISHED_BLACKSTONE_BUTTON) {
                buttonSlot = i;
                break;
            }
        }
        if (buttonSlot == -1) return;

        if (target != null) {
            if (mc.player.distanceTo(target) > range.get() || !target.isAlive()) target = null;
            if (mc.player.currentScreenHandler instanceof AnvilScreenHandler) mc.player.closeScreen();
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !FriendManager.INSTANCE.attack(player) || !player.isAlive() || mc.player.distanceTo(player) > range.get()) continue;

            if (target == null) {
                target = player;
            } else if (mc.player.distanceTo(target) > mc.player.distanceTo(player)) {
                target = player;
            }
        }

        if (target != null) {
            int prevSlot = mc.player.inventory.selectedSlot;

            if (placeButton.get()) {
                mc.player.inventory.selectedSlot = buttonSlot;
                BlockPos targetPos = target.getBlockPos();
                if (mc.world.getBlockState(targetPos.add(0, 0, 0)).isAir()) {
                    mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.DOWN, target.getBlockPos(), true));
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }

            mc.player.inventory.selectedSlot = anvilSlot;
            BlockPos targetPos = target.getBlockPos().up();

            PlayerUtils.placeBlock(targetPos.add(0, height.get(), 0), Hand.MAIN_HAND);

            mc.player.inventory.selectedSlot = prevSlot;
        }
    });

    @EventHandler
    private final Listener<OpenScreenEvent> onOpenScreen = new Listener<>(event -> {
        if (target != null && event.screen instanceof AnvilScreen) event.cancel();
    });
}
