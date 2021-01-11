/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listenable;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Module;
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

public class InvUtils implements Listenable {
    public static final int OFFHAND_SLOT = 45;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final FindItemResult findItemResult = new FindItemResult();
    private static final Deque<CustomPair> moveQueue = new ArrayDeque<>();
    private static final Queue<Integer> currentQueue = new LinkedList<>();

    public static void clickSlot(int slot, int button, SlotActionType action) {
        assert mc.interactionManager != null;
        assert mc.player != null;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, button, action, mc.player);
    }

    public static Hand getHand (Item item) {
        assert mc.player != null;
        Hand hand = Hand.MAIN_HAND;
        if (mc.player.getOffHandStack().getItem() == item) hand = Hand.OFF_HAND;
        return hand;
    }

    public static FindItemResult findItemWithCount(Item item) {
        assert mc.player != null;
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

    public static int findItemInHotbar(Item item, Predicate<ItemStack> isGood) {
        assert mc.player != null;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);
            if (itemStack.getItem() == item && isGood.test(itemStack)) return i;
        }

        return -1;
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

    @EventHandler
    private static final Listener<TickEvent.Pre> onTick = new Listener<>(event -> {
        if (mc.world == null || mc.player == null){
            currentQueue.clear();
            moveQueue.clear();
            return;
        }
        if (currentQueue.isEmpty() && !moveQueue.isEmpty()) {
            CustomPair pair = moveQueue.remove();
            currentQueue.addAll(pair.getRight());
        } else if (!currentQueue.isEmpty()) {
            currentQueue.forEach(slot -> clickSlot(slot, 0, SlotActionType.PICKUP));
            currentQueue.clear();
        }
    });

    public static void addSlots(List<Integer> slots, Class<? extends Module> klass){
        if (moveQueue.contains(new CustomPair(klass, slots)) || currentQueue.containsAll(slots)) return;
        if (!moveQueue.isEmpty() && canMove(klass)){
            moveQueue.addFirst(new CustomPair(klass, slots));
            onTick.invoke(new TickEvent.Pre());
        } else {
            moveQueue.add(new CustomPair(klass, slots));
        }
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
}