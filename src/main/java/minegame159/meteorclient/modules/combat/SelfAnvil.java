/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.PlayerUtils;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class SelfAnvil extends ToggleModule {
    public SelfAnvil() {
        super(Category.Combat, "self-anvil", "Automatically places an anvil to fill your hole.");
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

        int prevSlot = mc.player.inventory.selectedSlot;

        mc.player.inventory.selectedSlot = anvilSlot;
        BlockPos playerPos = mc.player.getBlockPos();

        PlayerUtils.placeBlock(playerPos.add(0, 2, 0), Hand.MAIN_HAND);

        mc.player.inventory.selectedSlot = prevSlot;
        toggle();
    });

    @EventHandler
    private final Listener<OpenScreenEvent> onOpenScreen = new Listener<>(event -> {
        if (event.screen instanceof AnvilScreen) event.cancel();
    });
}
