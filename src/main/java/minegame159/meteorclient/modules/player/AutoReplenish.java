/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.combat.AutoTotem;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

@InvUtils.Priority(priority = 1)
public class AutoReplenish extends Module {
    public AutoReplenish(){
        super(Category.Player, "auto-replenish", "Automatically refills items in your hotbar, main hand, or offhand.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
            .name("threshold")
            .description("The threshold of items left this actives at.")
            .defaultValue(8)
            .min(1)
            .sliderMax(63)
            .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("The tick delay to replenish your hotbar.")
            .defaultValue(1)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Boolean> offhand = sgGeneral.add(new BoolSetting.Builder()
            .name("offhand")
            .description("Whether or not to refill your offhand with items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> alert = sgGeneral.add(new BoolSetting.Builder()
            .name("alert")
            .description("Sends a client-side alert in chat when you run out of items.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> unstackable = sgGeneral.add(new BoolSetting.Builder()
            .name("unstackable")
            .description("Replenishes unstackable items.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> searchHotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("search-hotbar")
            .description("Uses items in your hotbar to replenish if they are the only ones left.")
            .defaultValue(true)
            .build()
    );

    private final Setting<List<Item>> excludedItems = sgGeneral.add(new ItemListSetting.Builder()
            .name("excluded-items")
            .description("Items that WILL NOT replenish.")
            .defaultValue(new ArrayList<>())
            .build()
    );

    private final Setting<Boolean> pauseInInventory = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-in-inventory")
            .description("Stops replenishing items when you are currently in your inventory.")
            .defaultValue(false)
            .build()
    );

    private final List<ItemStack> hotbar = new ArrayList<>();
    private ItemStack offhandStack;
    private ItemStack stack;
    private boolean sent = false;
    private int tickDelayLeft;

    @Override
    public void onActivate() {
        offhandStack = mc.player.getOffHandStack();
        tickDelayLeft = tickDelay.get();
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.currentScreen instanceof GenericContainerScreen) sent = false;
        if (mc.player.currentScreenHandler.getStacks().size() < 45 || (pauseInInventory.get() && mc.currentScreen instanceof InventoryScreen)) return;

        //Hotbar
        if (tickDelayLeft <= 0) {
            tickDelayLeft = tickDelay.get();
            for (int i = 0; i < 9; i++) {
                stack = mc.player.inventory.getStack(i);
                if (!excludedItems.get().contains(stack.getItem()) && stack.getItem() != Items.AIR) {
                    if (stack.isStackable()) {
                        if (stack.getCount() <= threshold.get()) {
                            addSlots(i, findItem(stack, i));
                        }
                    } else if (unstackable.get()) {
                        if (stack.isEmpty() && !hotbar.get(i).isStackable()) {
                            addSlots(i, findItem(hotbar.get(i), i));
                        }
                    }
                }
                hotbar.add(i, stack);
            }
            //Offhand
            if (offhand.get() && !Modules.get().get(AutoTotem.class).getLocked()) {
                if (mc.player.getOffHandStack().getCount() <= threshold.get()) {
                    addSlots(InvUtils.OFFHAND_SLOT, findItem(mc.player.getOffHandStack(), InvUtils.OFFHAND_SLOT));
                } else if (mc.player.getOffHandStack().isEmpty() || !offhandStack.isStackable()) {
                    addSlots(InvUtils.OFFHAND_SLOT, findItem(offhandStack, InvUtils.OFFHAND_SLOT));
                }
                offhandStack = mc.player.getOffHandStack();
            }
        }
        tickDelayLeft--;
    }

    private int findItem(ItemStack itemStack, int excludedSlot){
        int slot = -1;
        int size = 0;
        for (int i = mc.player.inventory.size() - 2; i >= (searchHotbar.get() ? 0 : 9); i--){
            if (i != excludedSlot && mc.player.inventory.getStack(i).getItem().equals(itemStack.getItem()) && ItemStack.areTagsEqual(itemStack, mc.player.inventory.getStack(i))){
                if (mc.player.inventory.getStack(i).getCount() > size){
                    slot = i;
                    size = mc.player.inventory.getStack(i).getCount();
                }
            }
        }
        return slot;
    }

    private void addSlots(int to, int from){
        if (to == -1) {
            return;
        } else if (from == -1){
            if (alert.get() && !sent) {
                ChatUtils.moduleWarning(this, "Items not found. Cannot refill.");
                sent = true;
            }
            return;
        }
        List<Integer> slots = new ArrayList<>();
        if (!mc.player.inventory.getCursorStack().isEmpty() && mc.player.inventory.getCursorStack().getItem().equals(mc.player.inventory.getStack(from).getItem())){
            slots.add(InvUtils.invIndexToSlotId(from));
        }
        slots.add(InvUtils.invIndexToSlotId(from));
        slots.add(InvUtils.invIndexToSlotId(to));
        slots.add(InvUtils.invIndexToSlotId(from));
        InvUtils.addSlots(slots, this.getClass());
    }
}
