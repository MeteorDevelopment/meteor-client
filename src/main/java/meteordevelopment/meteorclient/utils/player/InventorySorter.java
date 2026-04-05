/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import meteordevelopment.meteorclient.mixininterface.ISlot;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Tuple;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventorySorter {
    private final AbstractContainerScreen<?> screen;
    private final InvPart originInvPart;

    private boolean invalid;
    private List<Action> actions;
    private int timer, currentActionI;

    public InventorySorter(AbstractContainerScreen<?> screen, Slot originSlot) {
        this.screen = screen;

        this.originInvPart = getInvPart(originSlot);
        if (originInvPart == InvPart.Invalid || originInvPart == InvPart.Hotbar || screen instanceof PeekScreen) {
            invalid = true;
            return;
        }

        this.actions = new ArrayList<>();
        generateActions();
    }

    public boolean tick(int delay) {
        if (invalid) return true;
        if (currentActionI >= actions.size()) return true;

        if (timer >= delay) {
            timer = 0;
        } else {
            timer++;
            return false;
        }

        Action action = actions.get(currentActionI);
        InvUtils.move().fromId(action.from).toId(action.to);

        currentActionI++;
        return false;
    }

    private void generateActions() {
        // Find all slots and sort them
        List<MySlot> slots = new ArrayList<>();

        for (Slot slot : screen.getMenu().slots) {
            if (getInvPart(slot) == originInvPart)
                slots.add(new MySlot(((ISlot) slot).meteor$getIndex(), slot.getItem()));
        }

        slots.sort(Comparator.comparingInt(value -> value.id));

        // Generate actions
        generateStackingActions(slots);
        generateSortingActions(slots);
    }

    private void generateStackingActions(List<MySlot> slots) {
        // Generate a map for slots that can be stacked
        SlotMap slotMap = new SlotMap();

        for (MySlot slot : slots) {
            if (slot.itemStack.isEmpty() || !slot.itemStack.isStackable() || slot.itemStack.getCount() >= slot.itemStack.getMaxStackSize())
                continue;

            slotMap.get(slot.itemStack).add(slot);
        }

        // Stack previously found slots
        for (var entry : slotMap.map) {
            List<MySlot> slotsToStack = entry.getB();
            MySlot slotToStackTo = null;

            for (int i = 0; i < slotsToStack.size(); i++) {
                MySlot slot = slotsToStack.get(i);

                // Check if slotToStackTo is null and update it if it is
                if (slotToStackTo == null) {
                    slotToStackTo = slot;
                    continue;
                }

                // Generate action
                actions.add(new Action(slot.id, slotToStackTo.id));

                // Handle state when the two stacks can combine without any leftovers
                if (slotToStackTo.itemStack.getCount() + slot.itemStack.getCount() <= slotToStackTo.itemStack.getMaxStackSize()) {
                    slotToStackTo.itemStack = new ItemStack(slotToStackTo.itemStack.getItem(), slotToStackTo.itemStack.getCount() + slot.itemStack.getCount());
                    slot.itemStack = ItemStack.EMPTY;

                    if (slotToStackTo.itemStack.getCount() >= slotToStackTo.itemStack.getMaxStackSize())
                        slotToStackTo = null;
                }
                // Handle state when combining the two stacks produces leftovers
                else {
                    int needed = slotToStackTo.itemStack.getMaxStackSize() - slotToStackTo.itemStack.getCount();

                    slotToStackTo.itemStack = new ItemStack(slotToStackTo.itemStack.getItem(), slotToStackTo.itemStack.getMaxStackSize());
                    slot.itemStack = new ItemStack(slot.itemStack.getItem(), slot.itemStack.getCount() - needed);

                    slotToStackTo = null;
                    i--;
                }
            }
        }
    }

    private void generateSortingActions(List<MySlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            // Find best slot to move here
            MySlot bestSlot = null;

            for (int j = i; j < slots.size(); j++) {
                MySlot slot = slots.get(j);

                if (bestSlot == null) {
                    bestSlot = slot;
                    continue;
                }

                if (isSlotBetter(bestSlot, slot)) bestSlot = slot;
            }

            // Generate action
            if (!bestSlot.itemStack.isEmpty()) {
                MySlot toSlot = slots.get(i);

                int from = bestSlot.id;
                int to = toSlot.id;

                if (from != to) {
                    ItemStack temp = bestSlot.itemStack;
                    bestSlot.itemStack = toSlot.itemStack;
                    toSlot.itemStack = temp;

                    actions.add(new Action(from, to));
                }
            }
        }
    }

    private boolean isSlotBetter(MySlot best, MySlot slot) {
        ItemStack bestI = best.itemStack;
        ItemStack slotI = slot.itemStack;

        if (bestI.isEmpty() && !slotI.isEmpty()) return true;
        else if (!bestI.isEmpty() && slotI.isEmpty()) return false;

        int c = BuiltInRegistries.ITEM.getKey(bestI.getItem()).compareTo(BuiltInRegistries.ITEM.getKey(slotI.getItem()));
        if (c == 0) {
            if (slotI.getCount() != bestI.getCount()) return slotI.getCount() > bestI.getCount();
            if (slotI.getDamageValue() != bestI.getDamageValue())
                return slotI.getDamageValue() > bestI.getDamageValue();
        }

        return c > 0;
    }

    private InvPart getInvPart(Slot slot) {
        int i = ((ISlot) slot).meteor$getSlot();

        if (slot.container instanceof Inventory && (!(screen instanceof CreativeModeInventoryScreen) || ((ISlot) slot).meteor$getIndex() > 8)) {
            if (SlotUtils.isHotbar(i)) return InvPart.Hotbar;
            else if (SlotUtils.isMain(i)) return InvPart.Player;
        } else if ((screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen) && slot.container instanceof SimpleContainer) {
            return InvPart.Main;
        }

        return InvPart.Invalid;
    }

    private enum InvPart {
        Hotbar,
        Player,
        Main,
        Invalid
    }

    private static class MySlot {
        public final int id;
        public ItemStack itemStack;

        public MySlot(int id, ItemStack itemStack) {
            this.id = id;
            this.itemStack = itemStack;
        }
    }

    private static class SlotMap {
        private final List<Tuple<ItemStack, List<MySlot>>> map = new ArrayList<>();

        public List<MySlot> get(ItemStack itemStack) {
            for (Tuple<ItemStack, List<MySlot>> entry : map) {
                if (ItemStack.isSameItemSameComponents(itemStack, entry.getA())) {
                    return entry.getB();
                }
            }

            List<MySlot> list = new ArrayList<>();
            map.add(new Tuple<>(itemStack, list));
            return list;
        }
    }

    private record Action(int from, int to) {
    }
}
