/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.combat.AutoTotem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.Predicate;

public class InvUtils {
    public static final int OFFHAND_SLOT = 45;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final FindItemResult findItemResult = new FindItemResult();
    private static final Deque<CustomPair> moveQueue = new ArrayDeque<>();
    private static final Queue<Integer> currentQueue = new LinkedList<>();

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
        if (invIndex < 9) return 44 - (8 - invIndex);
        return invIndex;
    }

    public static class FindItemResult {
        public int slot, count;

        public boolean found() {
            return slot != -1;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private static void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null || mc.player.abilities.creativeMode){
            currentQueue.clear();
            moveQueue.clear();
            return;
        }

        if (!mc.player.inventory.getCursorStack().isEmpty() && mc.currentScreen == null && mc.player.currentScreenHandler.getStacks().size() > 44){
            int slot = mc.player.inventory.getEmptySlot();
            if (slot == -1) findItemWithCount(mc.player.inventory.getCursorStack().getItem());
            if (slot != -1) clickSlot(invIndexToSlotId(slot), 0, SlotActionType.PICKUP);
        }

        if (!moveQueue.isEmpty()) {
            if (currentQueue.isEmpty()) {
                CustomPair pair = moveQueue.remove();
                currentQueue.addAll(pair.getRight());
            }

            if (mc.player.currentScreenHandler.getStacks().size() > 44) {
                currentQueue.forEach(slot -> clickSlot(slot, 0, SlotActionType.PICKUP));
                currentQueue.clear();
            }
        }
    }

    public static void addSlots(List<Integer> slots, Class<? extends Module> klass){
        if (moveQueue.contains(new CustomPair(klass, slots)) || currentQueue.containsAll(slots)) return;

        if (klass == AutoTotem.class) {
            moveQueue.removeIf(pair -> pair.getRight().contains(45));
        }

        if (!moveQueue.isEmpty() && canMove(klass)){
            moveQueue.addFirst(new CustomPair(klass, slots));
        } else {
            moveQueue.add(new CustomPair(klass, slots));
        }

        onTick(new TickEvent.Pre());
    }

    public static boolean canMove(Class<? extends Module> klass){
        return getPrio(moveQueue.peek().getLeft()) < getPrio(klass);
    }

    private static int getPrio(Class<? extends Module> klass){
        if (klass == null) return -1;
        return klass.getAnnotation(Priority.class).priority();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Priority{
        int priority() default -1;
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
}