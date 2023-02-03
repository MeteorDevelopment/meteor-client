/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InvUtils {
    private static final Action ACTION = new Action();
    public static int previousSlot = -1;

    // Predicates

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getMainHandStack());
    }

    public static boolean testInMainHand(Item... items) {
        return testInMainHand(itemStack -> {
            for (var item : items) if (itemStack.isOf(item)) return true;
            return false;
        });
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getOffHandStack());
    }

    public static boolean testInOffHand(Item... items) {
        return testInOffHand(itemStack -> {
            for (var item : items) if (itemStack.isOf(item)) return true;
            return false;
        });
    }

    public static boolean testInHands(Predicate<ItemStack> predicate) {
        return testInMainHand(predicate) || testInOffHand(predicate);
    }

    public static boolean testInHands(Item... items) {
        return testInMainHand(items) || testInOffHand(items);
    }

    public static boolean testInHotbar(Predicate<ItemStack> predicate) {
        if (testInHands(predicate)) return true;

        for (int i = SlotUtils.HOTBAR_START; i < SlotUtils.HOTBAR_END; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) return true;
        }

        return false;
    }

    public static boolean testInHotbar(Item... items) {
        return testInHotbar(itemStack -> {
            for (var item : items) if (itemStack.isOf(item)) return true;
            return false;
        });
    }

    // Finding items

    public static FindItemResult findEmpty() {
        return find(ItemStack::isEmpty);
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (testInOffHand(isGood)) {
            return new FindItemResult(SlotUtils.OFFHAND, mc.player.getOffHandStack().getCount());
        }

        if (testInMainHand(isGood)) {
            return new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        if (mc.player == null) return new FindItemResult(0, 0);
        return find(isGood, 0, mc.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        if (mc.player == null) return new FindItemResult(0, 0);

        int slot = -1, count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }

        return new FindItemResult(slot, count);
    }

    public static FindItemResult findFastestTool(BlockState state) {
        float bestScore = 1;
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isSuitableFor(state)) continue;

            float score = stack.getMiningSpeedMultiplier(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }

        return new FindItemResult(slot, 1);
    }

    // Interactions

    public static boolean swap(int slot, boolean swapBack) {
        if (slot == SlotUtils.OFFHAND) return true;
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;
        else if (!swapBack) previousSlot = -1;

        mc.player.getInventory().selectedSlot = slot;
        ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        return true;
    }

    public static boolean swapBack() {
        if (previousSlot == -1) return false;

        boolean return_ = swap(previousSlot, false);
        previousSlot = -1;
        return return_;
    }

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

    public static void dropHand() {
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, ScreenHandler.EMPTY_SPACE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
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
            boolean hadEmptyCursor = mc.player.currentScreenHandler.getCursorStack().isEmpty();

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

            if (!isRecursive && hadEmptyCursor && preType == SlotActionType.PICKUP && preTwo && (preFrom != -1 && preTo != -1) && !mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
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
