/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

//Created by squidoodly 8/05/2020
//Updated by squidoodly 14/07/2020

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.OpenScreenEvent;
import minegame159.meteorclient.events.PostTickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

public class AutoReplenish extends ToggleModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
            .name("amount")
            .description("The amount this actives at")
            .defaultValue(8)
            .min(1)
            .sliderMax(63)
            .build()
    );

    private final Setting<Boolean> offhand = sgGeneral.add(new BoolSetting.Builder()
            .name("offhand")
            .description("Whether to re-fill your offhand")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> alert = sgGeneral.add(new BoolSetting.Builder()
            .name("alert")
            .description("Send messages in chat when you run out of items")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> unstackable = sgGeneral.add(new BoolSetting.Builder()
            .name("unstackable")
            .description("Replenishes unstackable items (only works for main hand and offhand)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> searchHotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("search-hotbar")
            .description("Refills items if they are in your hotbar.")
            .defaultValue(true)
            .build()
    );

    private final Setting<List<Item>> excludedItems = sgGeneral.add(new ItemListSetting.Builder()
            .name("excluded-items")
            .description("Items to not replenish.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> workInCont = sgGeneral.add(new BoolSetting.Builder()
            .name("work-in-containers")
            .description("Allows this to work while you are in containers.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> workInInv = sgGeneral.add(new BoolSetting.Builder()
            .name("work-in-inv")
            .description("Allows this to work in you inventory.")
            .defaultValue(true)
            .build()
    );

    private final List<Item> items = new ArrayList<>();

    private Item lastMainHand, lastOffHand;
    private int lastSlot;

    public AutoReplenish(){
        super(Category.Player, "auto-replenish", "Automatically fills your hotbar and offhand items");
    }

    @Override
    public void onActivate() {
        lastSlot = mc.player.inventory.selectedSlot;
    }

    @Override
    public void onDeactivate() {
        lastMainHand = lastOffHand = null;
    }

    @EventHandler
    private final Listener<PostTickEvent> onTick = new Listener<>(event -> {
        if (!workInCont.get() && !workInInv.get()) {
            if (mc.currentScreen instanceof HandledScreen<?>) return;
        } else if (workInCont.get() && !workInInv.get()) {
            if (mc.currentScreen instanceof HandledScreen<?> && mc.currentScreen instanceof InventoryScreen) return;
        } else if (!workInCont.get() && workInInv.get()) {
            if (mc.currentScreen instanceof HandledScreen<?> && !(mc.currentScreen instanceof InventoryScreen)) return;
        }

        // Hotbar, stackable items
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStack(i);
            InvUtils.FindItemResult result = InvUtils.findItemWithCount(stack.getItem());
            if(result.slot < i && i != mc.player.inventory.selectedSlot) continue;
            if(isUnstackable(stack.getItem()) || stack.getItem() == Items.AIR) continue;
            if (stack.getCount() < amount.get() && (stack.getMaxCount() > amount.get() || stack.getCount() < stack.getMaxCount())) {
                int slot = -1;
                if (searchHotbar.get()) {
                    for (int j = 0; j < 9; j++) {
                        if (mc.player.inventory.getStack(j).getItem() == stack.getItem() && ItemStack.areTagsEqual(stack, mc.player.inventory.getStack(j)) && mc.player.inventory.selectedSlot != j && i != j) {
                            slot = j;
                            break;
                        }
                    }
                }
                if (slot == -1) {
                    for (int j = 9; j < mc.player.inventory.main.size(); j++) {
                        if (mc.player.inventory.getStack(j).getItem() == stack.getItem() && ItemStack.areTagsEqual(stack, mc.player.inventory.getStack(j))) {
                            slot = j;
                            break;
                        }
                    }
                }
                if(slot != -1) {
                    moveItems(slot, i, true);
                }
            }
        }

        // OffHand, stackable items
        if (offhand.get()) {
            ItemStack stack = mc.player.getOffHandStack();
            if(stack.getItem() != Items.AIR) {
                if (stack.getCount() < amount.get() && (stack.getMaxCount() > amount.get() || stack.getCount() < stack.getMaxCount())) {
                    int slot = -1;
                    for (int i = 9; i < mc.player.inventory.main.size(); i++) {
                        if (mc.player.inventory.getStack(i).getItem() == stack.getItem() && ItemStack.areTagsEqual(stack, mc.player.inventory.getStack(i))) {
                            slot = i;
                            break;
                        }
                    }
                    if (searchHotbar.get() && slot == -1) {
                        for (int i = 0; i < 9; i++) {
                            if (mc.player.inventory.getStack(i).getItem() == stack.getItem() && ItemStack.areTagsEqual(stack, mc.player.inventory.getStack(i))) {
                                slot = i;
                                break;
                            }
                        }
                    }
                    if (slot != -1) {
                        moveItems(slot, InvUtils.OFFHAND_SLOT, true);
                    }
                }
            }
        }

        // MainHand, unstackable items
        if (unstackable.get()) {
            ItemStack mainHandItemStack = mc.player.getMainHandStack();
            if (mainHandItemStack.getItem() != lastMainHand && !excludedItems.get().contains(lastMainHand) && mainHandItemStack.isEmpty() && (lastMainHand != null && lastMainHand != Items.AIR) && isUnstackable(lastMainHand) && mc.player.inventory.selectedSlot == lastSlot) {
                int slot = findSlot(lastMainHand, lastSlot);
                if (slot != -1) moveItems(slot, lastSlot, false);
            }
            lastMainHand = mc.player.getMainHandStack().getItem();
            lastSlot = mc.player.inventory.selectedSlot;

            if (offhand.get()) {
                // OffHand, unstackable items
                ItemStack offHandItemStack = mc.player.getOffHandStack();
                if (offHandItemStack.getItem() != lastOffHand && !excludedItems.get().contains(lastOffHand) && offHandItemStack.isEmpty() && (lastOffHand != null && lastOffHand != Items.AIR) && isUnstackable(lastOffHand)) {
                    int slot = findSlot(lastOffHand, InvUtils.OFFHAND_SLOT);
                    if (slot != -1) moveItems(slot, InvUtils.OFFHAND_SLOT, false);
                }
                lastOffHand = mc.player.getOffHandStack().getItem();
            }
        }
    });

    @EventHandler
    private final Listener<OpenScreenEvent> onScreen = new Listener<>(event -> {
        if (mc.currentScreen instanceof HandledScreen<?>) {
            if (!(mc.currentScreen instanceof AbstractInventoryScreen)) items.clear();
            lastMainHand = lastOffHand = null;
        }
    });

    private void moveItems(int from, int to, boolean stackable) {
        InvUtils.clickSlot(InvUtils.invIndexToSlotId(from), 0, SlotActionType.PICKUP);
        InvUtils.clickSlot(InvUtils.invIndexToSlotId(to), 0, SlotActionType.PICKUP);
        if (stackable) InvUtils.clickSlot(InvUtils.invIndexToSlotId(from), 0, SlotActionType.PICKUP);
    }

    private int findSlot(Item item, int excludeSlot) {
        int slot = findItems(item, excludeSlot);

        if(slot == -1 && !items.contains(item)){
            if(alert.get()) {
                Chat.warning(this, "You are out of (highlight)%s(default). Cannot refill.", item.toString());
            }

            items.add(item);
        }

        return slot;
    }

    private int findItems(Item item, int excludeSlot) {
        int slot = -1;

        for (int i = searchHotbar.get() ? 0 : 9; i < mc.player.inventory.main.size(); i++) {
            if (i != excludeSlot && mc.player.inventory.main.get(i).getItem() == item && (!searchHotbar.get() || i != mc.player.inventory.selectedSlot)) {
                slot = i;
                return slot;
            }
        }

        return slot;
    }

    private boolean isUnstackable(Item item) {
        return item.getMaxCount() <= 1;
    }
}
