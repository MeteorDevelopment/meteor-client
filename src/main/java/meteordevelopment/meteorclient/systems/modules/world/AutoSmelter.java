/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceScreenHandler;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipePropertySet;
import net.minecraft.screen.AbstractFurnaceScreenHandler;

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

    private final Setting<Integer> fuelAmount = sgGeneral.add(new IntSetting.Builder()
        .name("fuel-amount")
        .description("Amount of fuel to insert in each furnace (1-64).")
        .defaultValue(32)
        .range(1, 64)
        .sliderMax(64)
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
    }

    /**
     * Checks and refills the fuel slot in the furnace.
     * Will add fuel up to the specified fuel amount setting.
     *
     * @param c The furnace screen handler
     */
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

        InvUtils.move().fromId(slot).toId(0);
    }

    /**
     * Checks and refills the fuel slot in the furnace.
     *
     * @param c The furnace screen handler
     */
    private void checkFuel(AbstractFurnaceScreenHandler c) {
        ItemStack fuelStack = c.slots.get(1).getStack();

        // If the furnace is burning, don't do anything
        if (c.getFuelProgress() > 0) return;
        if (!fuelStack.isEmpty()) return;

        // Check if we already have the maximum amount of fuel
        if (!fuelStack.isEmpty() && fuelStack.getCount() >= fuelAmount.get()) return;

        // Calculate how much more fuel we need
        int neededFuelCount = fuelStack.isEmpty() ? fuelAmount.get() : fuelAmount.get() - fuelStack.getCount();

        // Find fuel in inventory
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
            return;
        }

        InvUtils.move(neededFuelCount).fromId(slot).toId(1);
    }

    /**
     * Takes the smelted results from the furnace output slot.
     * Disables the module if inventory is full and can't take results.
     *
     * @param c The furnace screen handler
     */
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
