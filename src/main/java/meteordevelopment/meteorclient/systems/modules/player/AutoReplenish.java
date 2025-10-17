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
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.List;

public class AutoReplenish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> minCount = sgGeneral.add(new IntSetting.Builder()
        .name("min-count")
        .description("Replenish a slot when it reaches this item count.")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 63)
        .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("How long in ticks to wait between replenishing your hotbar.")
        .defaultValue(1)
        .min(0)
        .build()
    );

    private final Setting<Boolean> offhand = sgGeneral.add(new BoolSetting.Builder()
        .name("offhand")
        .description("Whether or not to replenish items in your offhand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> unstackable = sgGeneral.add(new BoolSetting.Builder()
        .name("unstackable")
        .description("Replenish unstackable items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sameEnchants = sgGeneral.add(new BoolSetting.Builder()
        .name("same-enchants")
        .description("Only replace unstackables with items that have the same enchants.")
        .defaultValue(true)
        .visible(unstackable::get)
        .build()
    );

    private final Setting<Boolean> searchHotbar = sgGeneral.add(new BoolSetting.Builder()
        .name("search-hotbar")
        .description("Combine stacks in your hotbar/offhand as a last resort.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<Item>> excludedItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("excluded-items")
        .description("Items that won't be replenished.")
        .build()
    );

    /**
     * Represents the items the player had last tick. Indices 0-8 represent the
     * hotbar from left to right, index 9 represents the player's offhand
     */
    private final ItemStack[] items = new ItemStack[10];
    private boolean prevHadOpenScreen;
    private int tickDelayLeft;

    public AutoReplenish() {
        super(Categories.Player, "auto-replenish", "Automatically refills items in your hotbar, main hand, or offhand.");

        Arrays.fill(items, Items.AIR.getDefaultStack());
    }

    @Override
    public void onActivate() {
        fillItems();
        tickDelayLeft = tickDelay.get();
        prevHadOpenScreen = mc.currentScreen != null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.currentScreen == null && prevHadOpenScreen) {
            fillItems();
        }

        prevHadOpenScreen = mc.currentScreen != null;
        if (mc.player.currentScreenHandler.getStacks().size() != 46 || mc.currentScreen != null) return;

        if (tickDelayLeft > 0) {
            tickDelayLeft--;
            return;
        }

        // Hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            checkSlot(i, stack);
        }

        // Offhand
        if (offhand.get() && !Modules.get().get(AutoTotem.class).isLocked()) {
            ItemStack stack = mc.player.getOffHandStack();
            checkSlot(9, stack);
        }

        tickDelayLeft = tickDelay.get();
    }

    private void checkSlot(int slot, ItemStack stack) {
        ItemStack prevStack = items[slot];
        items[slot] = stack.copy();

        if (slot == 9) slot = SlotUtils.OFFHAND;
        
        if (excludedItems.get().contains(stack.getItem())) return;
        if (excludedItems.get().contains(prevStack.getItem())) return;

        int fromSlot = -1;

        // If there are still items left in the stack, but it just crossed the threshold
        if (stack.isStackable() && !stack.isEmpty() && stack.getCount() <= minCount.get()) {
            fromSlot = findItem(stack, slot, minCount.get() - stack.getCount() + 1, true);
        }

        // If the stack just went from above the threshold to empty in a single tick
        // this can happen if the threshold is set low enough while using modules that
        // place many blocks per tick, like surround or holefiller
        if (prevStack.isStackable() && stack.isEmpty() && !prevStack.isEmpty()) {
            fromSlot = findItem(prevStack, slot, minCount.get() - stack.getCount() + 1, false);
        }

        // Unstackable items
        if (unstackable.get() && !prevStack.isStackable() && stack.isEmpty() && !prevStack.isEmpty()) {
            fromSlot = findItem(prevStack, slot, 1, false);
        }

        // eliminate occasional loops when moving items from hotbar to itself
        if (fromSlot == mc.player.getInventory().getSelectedSlot() || fromSlot == SlotUtils.OFFHAND) return;
        if (fromSlot < 9 && fromSlot < slot && slot != mc.player.getInventory().getSelectedSlot() && slot != SlotUtils.OFFHAND) return;

        InvUtils.move().from(fromSlot).to(slot);
    }

    private int findItem(ItemStack lookForStack, int excludedSlot, int goodEnoughCount, boolean mustCombine) {
        int slot = -1;
        int count = 0;

        for (int i = mc.player.getInventory().size() - 2; i >= (searchHotbar.get() ? 0 : 9); i--) {
            if (i == excludedSlot) continue;

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() != lookForStack.getItem()) continue;

            if (mustCombine && !ItemStack.areItemsAndComponentsEqual(lookForStack, stack)) continue;
            if (sameEnchants.get() && !stack.getEnchantments().equals(lookForStack.getEnchantments())) continue;

            if (stack.getCount() > count) {
                slot = i;
                count = stack.getCount();

                if (count >= goodEnoughCount) break;
            }
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
