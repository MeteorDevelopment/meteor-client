/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.function.Predicate;

import static minegame159.meteorclient.utils.Utils.mc;

public class InvUtils {
    private static final Action ACTION = new Action();

    private static final FindItemResult findItemResult = new FindItemResult();

    // Interactions

    public static Action move() {
        ACTION.type = SlotActionType.PICKUP;
        ACTION.two = true;
        return ACTION;
    }

    public static Action click() {
        ACTION.type = SlotActionType.PICKUP;
        return ACTION;
    }

    public static Action quickMove() {
        ACTION.type = SlotActionType.QUICK_MOVE;
        return ACTION;
    }

    public static Action drop() {
        ACTION.type = SlotActionType.THROW;
        ACTION.data = 1;
        return ACTION;
    }

    // Hand

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

    // Find item

    public static FindItemResult findItemWithCount(Item item) {
        findItemResult.slot = -1;
        findItemResult.count = 0;

        for (int i = 0; i <= 35; i++) {
            ItemStack itemStack = mc.player.inventory.getStack(i);

            if (itemStack.getItem() == item) {
                if (!findItemResult.found()) findItemResult.slot = i;
                findItemResult.count += itemStack.getCount();
            }
        }

        return findItemResult;
    }

    // Find slot

    //Hotbar
    public static int findItemInHotbar(Predicate<ItemStack> isGood) {
        return findItem(isGood, 0, 8);
    }

    public static int findItemInHotbar(Item item) {
        return findItemInHotbar(itemStack -> itemStack.getItem() == item);
    }

    //Main
    public static int findItemInInventory(Predicate<ItemStack> isGood) {
        return findItem(isGood, 9, 35);
    }

    public static int findItemInInventory(Item item) {
        return findItemInInventory(itemStack -> itemStack.getItem() == item);
    }

    // Whole
    public static int findItemInWhole(Predicate<ItemStack> isGood) {
        return findItem(isGood, 0, 35);
    }

    public static int findItemInWhole(Item item) {
        return findItemInWhole(itemStack -> itemStack.getItem() == item);
    }

    private static int findItem(Predicate<ItemStack> isGood, int startSlot, int endSlot) {
        for (int i = startSlot; i <= endSlot; i++) {
            if (isGood.test(mc.player.inventory.getStack(i))) return i;
        }
        return -1;
    }

    public static class FindItemResult {
        public int slot, count;

        public boolean found() {
            return slot != -1;
        }
    }

    public static class Action {
        private SlotActionType type = null;
        private boolean two = false;
        private int from = -1;
        private int to = -1;
        private int data = 0;

        private boolean isRecursive = false;

        private Action() {}

        // From

        public Action fromId(int id) {
            from = id;
            return this;
        }

        public Action from(int index) {
            return fromId(SlotUtils.indexToId(index));
        }

        public Action fromHotbar(int i) {
            return from(SlotUtils.HOTBAR_START + i);
        }

        public Action fromOffhand() {
            return from(SlotUtils.OFFHAND);
        }

        public Action fromMain(int i) {
            return from(SlotUtils.MAIN_START + i);
        }

        public Action fromArmor(int i) {
            return from(SlotUtils.ARMOR_START + (3 - i));
        }

        // To

        public void toId(int id) {
            to = id;
            run();
        }

        public void to(int index) {
            toId(SlotUtils.indexToId(index));
        }

        public void toHotbar(int i) {
            to(SlotUtils.HOTBAR_START + i);
        }

        public void toOffhand() {
            to(SlotUtils.OFFHAND);
        }

        public void toMain(int i) {
            to(SlotUtils.MAIN_START + i);
        }

        public void toArmor(int i) {
            to(SlotUtils.ARMOR_START + (3 - i));
        }

        // Slot

        public void slotId(int id) {
            from = to = id;
            run();
        }

        public void slot(int index) {
            slotId(SlotUtils.indexToId(index));
        }

        public void slotHotbar(int i) {
            slot(SlotUtils.HOTBAR_START + i);
        }

        public void slotOffhand() {
            slot(SlotUtils.OFFHAND);
        }

        public void slotMain(int i) {
            slot(SlotUtils.MAIN_START + i);
        }

        public void slotArmor(int i) {
            slot(SlotUtils.ARMOR_START + (3 - i));
        }

        // Other

        private void run() {
            boolean hadEmptyCursor = mc.player.inventory.getCursorStack().isEmpty();

            if (type != null && from != -1 && to != -1) {
               click(from);
               if (two) click(to);
            }

            SlotActionType preType = type;
            boolean preTwo = two;
            int preFrom = from;
            int preTo = to;

            type = null;
            two = false;
            from = -1;
            to = -1;
            data = 0;

            if (!isRecursive && hadEmptyCursor && preType == SlotActionType.PICKUP && preTwo && (preFrom != -1 && preTo != -1) && !mc.player.inventory.getCursorStack().isEmpty()) {
                isRecursive = true;
                InvUtils.click().slotId(preFrom);
                isRecursive = false;
            }
        }

        private void click(int id) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, data, type, mc.player);
        }
    }
}
