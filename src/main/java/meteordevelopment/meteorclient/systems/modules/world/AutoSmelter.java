/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceScreenHandler;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipePropertySet;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class AutoSmelter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> fuelItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("fuel-items")
        .description("Items to use as fuel")
        .defaultValue(Items.COAL, Items.CHARCOAL)
        .filter(this::fuelItemFilter)
        .bypassFilterWhenSavingAndLoading()
        .build()
    );

    private final Setting<Integer> fuelItemsPerRefill = sgGeneral.add(new IntSetting.Builder()
        .name("fuel-items-per-refill")
        .description("How many fuel items to put into the furnace each time it refills")
        .defaultValue(64)
        .range(1,64)
        .sliderRange(1,64)
        .build()
    );

    private final Setting<List<Item>> smeltableItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("smeltable-items")
        .description("Items to smelt")
        .defaultValue(Items.IRON_ORE, Items.GOLD_ORE, Items.COPPER_ORE, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD)
        .filter(this::smeltableItemFilter)
        .bypassFilterWhenSavingAndLoading()
        .build()
    );

    private final Setting<Boolean> disableWhenOutOfItems = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-out-of-items")
        .description("Disable the module when you run out of items")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> closeAfterDeposit = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-close")
        .defaultValue(false)
        .build()
    );

    public AutoSmelter() {
        super(Categories.World, "auto-smelter", "Automatically smelts items from your inventory");
    }

    private boolean fuelItemFilter(Item item) {
        if (!Utils.canUpdate()) return false;

        return mc.getNetworkHandler().getFuelRegistry().getFuelItems().contains(item);
    }

    private boolean smeltableItemFilter(Item item) {
        return mc.world != null && mc.world.getRecipeManager().getPropertySet(RecipePropertySet.FURNACE_INPUT).canUse(item.getDefaultStack());
    }

    public void tick(AbstractFurnaceScreenHandler c) {
        // Limit actions to happen every n ticks
        if (mc.player.age % 10 == 0) return;

        // Check for fuel
        checkFuel(c);

        // Take the smelted results
        takeResults(c);

        // Insert new items
        insertItems(c);

        if (closeAfterDeposit.get() && mc.currentScreen != null) {
            mc.currentScreen.close();
        }
    }

    private void insertItems(AbstractFurnaceScreenHandler c) {
        ItemStack inputItemStack = c.slots.getFirst().getStack();
        if (!inputItemStack.isEmpty()) return;

        int slot = -1;

        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getStack();
            if (!((IAbstractFurnaceScreenHandler) c).meteor$isItemSmeltable(item)) continue;
            if (!smeltableItems.get().contains(item.getItem())) continue;
            if (!smeltableItemFilter(item.getItem())) continue;

            slot = i;
            break;
        }

        if (disableWhenOutOfItems.get() && slot == -1) {
            error("You do not have any items in your inventory that can be smelted. Disabling.");
            toggle();
            return;
        }

        if (slot == -1) return;

        InvUtils.move().fromId(slot).toId(0);
        c.slots.getFirst().getStack();
    }

    private boolean checkFuel(AbstractFurnaceScreenHandler c) {
        ItemStack fuelStack = c.slots.get(1).getStack();

        if (c.getFuelProgress() > 0) return false;
        if (!fuelStack.isEmpty()) return false;

        int slot = -1;
        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getStack();
            if (!fuelItems.get().contains(item.getItem())) continue;
            if (!fuelItemFilter(item.getItem())) continue;

            slot = i;
            break;
        }

        if (disableWhenOutOfItems.get() && slot == -1) {
            error("You do not have any fuel in your inventory. Disabling.");
            toggle();
            return false;
        }

        if (slot == -1) return false;

        ItemStack sourceStack = c.slots.get(slot).getStack();
        int moveCount = Math.min(fuelItemsPerRefill.get(), Math.min(sourceStack.getCount(), c.slots.get(1).getMaxItemCount(sourceStack)));

        if (moveCount <= 0) return false;

        return moveFuelItems(slot, moveCount);
    }

    private boolean moveFuelItems(int fromId, int amount) {
        if (amount <= 0 || mc.player == null || mc.interactionManager == null) return false;
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) return false;

        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, fromId, 0, SlotActionType.PICKUP, mc.player);

        for (int i = 0; i < amount; i++) {
            if (mc.player.currentScreenHandler.getCursorStack().isEmpty()) break;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 1, 1, SlotActionType.PICKUP, mc.player);
        }

        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, fromId, 0, SlotActionType.PICKUP, mc.player);
        }

        return !mc.player.currentScreenHandler.getSlot(1).getStack().isEmpty();
    }

    private void takeResults(AbstractFurnaceScreenHandler c) {
        ItemStack resultStack = c.slots.get(2).getStack();
        if (resultStack.isEmpty()) return;

        InvUtils.shiftClick().slotId(2);

        if (!resultStack.isEmpty()) {
            error("Your inventory is full. Disabling.");
            toggle();
        }
    }
}
