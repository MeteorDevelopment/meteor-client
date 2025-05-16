/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

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
import java.util.function.Predicate;

public class AutoSmelter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<List<Item>> fuelItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("fuel-items")
        .description("Items to use as fuel")
        .defaultValue(
            Items.COAL,
            Items.CHARCOAL
        )
        .filter(this::isFuelItem)
        .bypassFilterWhenSavingAndLoading()
        .build()
    );

    private final Setting<List<Item>> smeltableItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("smeltable-items")
        .description("Items to smelt")
        .defaultValue(
            Items.IRON_ORE,
            Items.GOLD_ORE,
            Items.COPPER_ORE,
            Items.RAW_IRON,
            Items.RAW_COPPER,
            Items.RAW_GOLD
        )
        .filter(this::isSmeltableItem)
        .bypassFilterWhenSavingAndLoading()
        .build()
    );

    private final Setting<Boolean> disableWhenOutOfItems = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-out-of-items")
        .description("Disable the module when you run out of items")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smartFuelUsage = sgGeneral.add(new BoolSetting.Builder()
        .name("smart-fuel-usage")
        .description("Only uses the amount of fuel needed to smelt available items")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> fuelAmount = sgGeneral.add(new IntSetting.Builder()
        .name("fuel-amount")
        .description("Amount of fuel to insert in each furnace (1-64).")
        .defaultValue(64)
        .range(1, 64)
        .sliderMax(64)
        .visible(() -> !smartFuelUsage.get())
        .build()
    );

    public AutoSmelter() {
        super(Categories.World, "auto-smelter", "Automatically smelts items from your inventory");
    }

    /**
     * Returns true if the item is valid fuel.
     */
    private boolean isFuelItem(Item item) {
        return Utils.canUpdate() && mc.getNetworkHandler().getFuelRegistry().getFuelItems().contains(item);
    }

    /**
     * Returns true if the item can be smelted.
     */
    private boolean isSmeltableItem(Item item) {
        return mc.world != null && mc.world.getRecipeManager()
            .getPropertySet(RecipePropertySet.FURNACE_INPUT)
            .canUse(item.getDefaultStack());
    }

    /**
     * Returns how many items can be smelted per fuel item.
     */
    private double getFuelEfficiency(Item item) {
        int burnTime = mc.getNetworkHandler().getFuelRegistry().getFuelTicks(new ItemStack(item));
        // Each item smelts in 200 ticks
        return burnTime > 0 ? burnTime / 200.0 : 0.0;
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

        int slot = findSlot(c, smeltableItems.get(), this::isSmeltableItem);
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
        if (!fuelStack.isEmpty() || c.getFuelProgress() > 0) return;

        // If smart fuel is enabled, calculate how much fuel we need based on smeltable items
        int neededFuelCount = fuelAmount.get();

        if (smartFuelUsage.get()) {
            int smeltableItemCount = countSmeltableItems(c);
            if (smeltableItemCount == 0) return;

            // Calculate fuel needed based on the burn rate of available fuel
            neededFuelCount = calculateRequiredFuel(smeltableItemCount);
            if (neededFuelCount == 0) return;
        }

        // Find fuel in inventory
        int slot = findSlot(c, fuelItems.get(), this::isFuelItem);
        if (disableWhenOutOfItems.get() && slot == -1) {
            error("You do not have any fuel in your inventory. Disabling.");
            toggle();
            return;
        }

        // Move the calculated amount of fuel to the furnace
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

    /**
     * Finds a slot in the player's inventory containing an item from the specified list
     * that also satisfies the given filter condition.
     *
     * @param c      The furnace screen handler
     * @param items  List of items to search for
     * @param filter Additional condition the item must satisfy
     * @return The slot ID containing a matching item, or -1 if none found
     */
    private int findSlot(AbstractFurnaceScreenHandler c, List<Item> items, Predicate<Item> filter) {
        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getStack();
            if (items.contains(item.getItem()) && filter.test(item.getItem())) return i;
        }
        return -1;
    }

    /**
     * Counts how many smeltable items are available for a single smelting operation
     *
     * @param c The furnace screen handler
     * @return The count of smeltable items for one batch (max 64)
     */
    private int countSmeltableItems(AbstractFurnaceScreenHandler c) {
        // First check the input slot
        ItemStack inputStack = c.slots.get(0).getStack();
        if (!inputStack.isEmpty() && isSmeltableItem(inputStack.getItem())) {
            // If there's already something in the input slot, only consider that
            return Math.min(inputStack.getCount(), 64);
        }

        // Otherwise, find the first stack of smeltable items
        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack stack = c.slots.get(i).getStack();
            if (!stack.isEmpty() && smeltableItems.get().contains(stack.getItem()) && isSmeltableItem(stack.getItem())) {
                return Math.min(stack.getCount(), 64);
            }
        }

        return 0;
    }

    /**
     * Calculates how much fuel is needed based on the number of items to smelt
     *
     * @param itemCount Number of items to smelt
     * @return Number of fuel items needed
     */
    private int calculateRequiredFuel(int itemCount) {
        // If no items to smelt, don't need fuel
        if (itemCount <= 0) return 0;

        // Get the first available fuel item
        Item fuelItem = fuelItems.get().stream()
            .filter(item -> mc.player.currentScreenHandler.slots.stream()
                .anyMatch(slot -> slot.getStack().getItem() == item))
            .findFirst()
            .orElse(null);

        if (fuelItem == null) return 0;

        // Get efficiency from map with default value of 8
        double itemsPerFuel = Math.max(getFuelEfficiency(fuelItem), 8.0);

        // Calculate how many fuel items are needed
        int fuelNeeded = (int) Math.ceil(itemCount / itemsPerFuel);

        // Cap at the max fuel amount setting
        return Math.min(fuelNeeded, fuelAmount.get());
    }
}
