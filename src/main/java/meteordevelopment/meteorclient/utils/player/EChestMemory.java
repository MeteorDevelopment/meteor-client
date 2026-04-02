/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.BlockActivateEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EnderChestBlock;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EChestMemory {
    public static final NonNullList<ItemStack> ITEMS = NonNullList.withSize(27, ItemStack.EMPTY);
    private static int echestOpenedState;
    private static boolean isKnown = false;

    private EChestMemory() {
    }

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(EChestMemory.class);
    }

    @EventHandler
    private static void onBlockActivate(BlockActivateEvent event) {
        if (event.blockState.getBlock() instanceof EnderChestBlock && echestOpenedState == 0) echestOpenedState = 1;
    }

    @EventHandler
    private static void onOpenScreenEvent(OpenScreenEvent event) {
        if (echestOpenedState == 1 && event.screen instanceof ContainerScreen) {
            echestOpenedState = 2;
            return;
        }
        if (echestOpenedState == 0) return;

        if (!(mc.screen instanceof ContainerScreen)) return;
        ChestMenu container = ((ContainerScreen) mc.screen).getMenu();
        if (container == null) return;
        Container inv = container.getContainer();

        for (int i = 0; i < 27; i++) {
            ITEMS.set(i, inv.getItem(i));
        }
        isKnown = true;

        echestOpenedState = 0;
    }

    @EventHandler
    private static void onLeaveEvent(GameLeftEvent event) {
        ITEMS.clear();
        isKnown = false;
    }

    public static boolean isKnown() {
        return isKnown;
    }
}
