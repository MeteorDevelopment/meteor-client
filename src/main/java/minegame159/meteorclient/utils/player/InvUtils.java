/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

public class InvUtils {
    public static final int OFFHAND_SLOT = 45;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final FindItemResult findItemResult = new FindItemResult();
    private static final Deque<Long> moveQueue = new ArrayDeque<>();
    private static Long currentMove;

    public static void clickSlot(int slot, int button, SlotActionType action) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, action, mc.player);
    }

    public static Hand getHand(Item item) {
        Hand hand = Hand.MAIN_HAND;
        if (mc.player.getOffHandStack().getItem() == item) hand = Hand.OFF_HAND;
        return hand;
    }

    public static Hand getHand(Predicate<ItemStack> isGood) {
        Hand hand = null;
        if (isGood.test(mc.player.getMainHandStack())) hand = Hand.MAIN_HAND;
        else if (isGood.test(mc.player.getOffHandStack())) hand = Hand.OFF_HAND;

        return hand;
    }

    public static FindItemResult findItemWithCount(Item item) {
        findItemResult.slot = -1;
        findItemResult.count = 0;

        for (int i = 0; i < mc.player.inventory.size(); i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);

            if (itemStack.getItem() == item) {
                if (!findItemResult.found()) findItemResult.slot = i;
                findItemResult.count += itemStack.getCount();
            }
        }

        return findItemResult;
    }

    public static int invIndexToSlotId(int invIndex) {
        if (invIndex < 9 && invIndex != -1) return 44 - (8 - invIndex);
        return invIndex;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private static void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null || mc.player.abilities.creativeMode) {
            moveQueue.clear();
            return;
        }

        if (!mc.player.inventory.getCursorStack().isEmpty() && mc.currentScreen == null && mc.player.currentScreenHandler.getStacks().size() == 46) {
            int slot = findItemWithCount(mc.player.inventory.getCursorStack().getItem()).slot;
            if (slot == -1) slot = mc.player.inventory.getEmptySlot();
            if (slot != -1) clickSlot(invIndexToSlotId(slot), 0, SlotActionType.PICKUP);
        }

        if (!moveQueue.isEmpty()) {
            if (mc.player.currentScreenHandler.getStacks().size() == 46) {
                currentMove = moveQueue.remove();
                clickSlot(unpackLongFrom(currentMove), 0, SlotActionType.PICKUP);
                clickSlot(unpackLongTo(currentMove), 0, SlotActionType.PICKUP);
                clickSlot(unpackLongFrom(currentMove), 0, SlotActionType.PICKUP);
            }
        }
    }

    public static void addSlots(int mode, int to, int from, int prio) {
        Long action = Utils.packLong(mode, to, from, prio);
        if (moveQueue.contains(action)) return;

        moveQueue.removeIf(entry -> (actionContains(entry, unpackLongTo(action)) || actionContains(entry, unpackLongFrom(action))) && canMove(entry, action));

        boolean isEmpty = moveQueue.isEmpty();
        if (moveQueue.isEmpty() || canMove(moveQueue.peek(), action)) {
            moveQueue.addFirst(action);
        } else {
            moveQueue.add(action);
        }

        if (isEmpty) onTick(new TickEvent.Pre());
    }

    public static boolean canMove(Long first, Long action) {
        return unpackLongPrio(first) < unpackLongPrio(action);
    }

    //Whole
    public static int findItemInAll(Item item) {
        return findItemInHotbar(item, itemStack -> true);
    }

    public static int findItemInAll(Item item, Predicate<ItemStack> isGood) {
        return findItem(item, isGood, mc.player.inventory.size());
    }

    public static int findItemInAll(Predicate<ItemStack> isGood) {
        return findItem(null, isGood, mc.player.inventory.size());
    }

    //Hotbar
    public static int findItemInHotbar(Item item) {
        return findItemInHotbar(item, itemStack -> true);
    }

    public static int findItemInHotbar(Item item, Predicate<ItemStack> isGood) {
        return findItem(item, isGood, 9);
    }

    public static int findItemInHotbar(Predicate<ItemStack> isGood) {
        return findItem(null, isGood, 9);
    }

    //Main
    public static int findItemInMain(Item item) {
        return findItemInHotbar(item, itemStack -> true);
    }

    public static int findItemInMain(Item item, Predicate<ItemStack> isGood) {
        return findItem(item, isGood, mc.player.inventory.main.size());
    }

    public static int findItemInMain(Predicate<ItemStack> isGood) {
        return findItem(null, isGood, mc.player.inventory.main.size());
    }

    private static int findItem(Item item, Predicate<ItemStack> isGood, int size) {
        for (int i = 0; i < size; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if ((item == null || itemStack.getItem() == item) && isGood.test(itemStack)) return i;
        }
        return -1;
    }

    public static void swap(int slot) {
        if (slot != mc.player.inventory.selectedSlot && slot >= 0 && slot < 9) mc.player.inventory.selectedSlot = slot;
    }

    private static boolean actionContains(long l, int i) {
        return i == unpackLongTo(l) || i == unpackLongFrom(l);
    }

    private static int unpackLongMode(long l) {
        return Utils.unpackLong1(l);
    }

    private static int unpackLongTo(long l) {
        return Utils.unpackLong2(l);
    }

    private static int unpackLongFrom(long l) {
        return Utils.unpackLong3(l);
    }

    private static int unpackLongPrio(long l) {
        return Utils.unpackLong4(l);
    }

    public static class FindItemResult {
        public int slot, count;

        public boolean found() {
            return slot != -1;
        }
    }
}