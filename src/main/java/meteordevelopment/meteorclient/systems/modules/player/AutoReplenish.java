/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class AutoReplenish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("How many items should be left before refilling.")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 63)
        .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long to wait before refilling after hitting the threshold.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> offhand = sgGeneral.add(new BoolSetting.Builder()
        .name("offhand")
        .description("Whether or not to refill items in your offhand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> unstackable = sgGeneral.add(new BoolSetting.Builder()
        .name("unstackable")
        .description("Replenishes unstackable items like tools.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sameEnchants = sgGeneral.add(new BoolSetting.Builder()
        .name("same-enchants")
        .description("Only replace unstackables with items with the same enchants")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> searchHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("search-hotbar")
        .description("Use items in your hotbar  if they are the only ones left.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<Item>> excludedItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("excluded-items")
        .description("Items that will not be replenished.")
        .build()
    );

    private final ItemStack[] items = new ItemStack[10];
    private boolean prevHadOpenScreen;
    private double tickDelayLeft;

    public AutoReplenish() {
        super(Categories.Player, "auto-replenish", "Automatically refills items in your hotbar, main hand, or offhand.");

        for (int i = 0; i < items.length; i++) items[i] = Items.AIR.getDefaultStack();
    }

    @Override
    public void onActivate() {
        fillItems();
        tickDelayLeft = 0;
        prevHadOpenScreen = mc.currentScreen != null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.currentScreen == null && prevHadOpenScreen) {
            fillItems();
        }

        prevHadOpenScreen = mc.currentScreen != null;
        if (mc.player.currentScreenHandler.getStacks().size() != 46 || mc.currentScreen != null) return;

        tickDelayLeft -= TickRate.INSTANCE.getTickRate() / 20.0;
        if (tickDelayLeft > 0) return;

        // Hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            checkSlot(i, stack);
        }

        // Offhand
        if (offhand.get() && !Modules.get().get(AutoTotem.class).isLocked()) {
            ItemStack stack = mc.player.getOffHandStack();
            checkSlot(SlotUtils.OFFHAND, stack);
        }

        tickDelayLeft = tickDelay.get();
    }

    private void checkSlot(int slot, ItemStack currentStack) {
        int itemsIndex = slot == SlotUtils.OFFHAND ? 9 : slot;
        ItemStack prevStack = items[itemsIndex];
        int fromSlot = -1;

        // If you still have some left in the stack and just now crossed the threshold
        if (!currentStack.isEmpty() && currentStack.isStackable() && currentStack.getCount() <= threshold.get()) {
            if (excludedItems.get().contains(currentStack.getItem())) return;
            fromSlot = findItem(currentStack, slot, threshold.get() - currentStack.getCount() + 1);
        }

        // If you just went from above the threshold to zero in a single tick, which is possible
        // with a low enough threshold and using modules that place multiple blocks per tick,
        // e.g. surround or holefiller using obsidian
        if (currentStack.isEmpty() && !prevStack.isEmpty() && !prevStack.isStackable() && unstackable.get()) {
            if (excludedItems.get().contains(prevStack.getItem())) return;
            fromSlot = findItem(prevStack, slot, threshold.get() - currentStack.getCount() + 1);
        }

        // Unstackable items
        if (currentStack.isEmpty() && !prevStack.isEmpty() && prevStack.isStackable()) {
            if (excludedItems.get().contains(prevStack.getItem())) return;
            fromSlot = findItem(prevStack, slot, 1);
        }

        InvUtils.move().from(fromSlot).to(slot);
        items[itemsIndex] = currentStack.copy();
    }

    private int findItem(ItemStack itemStack, int excludedSlot, int minimumCount) {
        int slot = -1;
        int count = 0;

        for (int i = mc.player.getInventory().size() - 2; i >= (searchHotbar.get() ? 0 : 9); i--) {
            if (i == excludedSlot) continue;

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != itemStack.getItem()) continue;

            if (sameEnchants.get() && !stack.getEnchantments().equals(itemStack.getEnchantments())) continue;

            if (stack.getCount() <= count) continue;

            slot = i;
            count = stack.getCount();

            if (count >= minimumCount) break;
        }

        return slot;
    }

    private void fillItems() {
        for (int i = 0; i < 9; i++) {
            items[i] = mc.player.getInventory().getStack(i).copy();
        }

        items[9] = mc.player.getOffHandStack().copy();
    }
}
