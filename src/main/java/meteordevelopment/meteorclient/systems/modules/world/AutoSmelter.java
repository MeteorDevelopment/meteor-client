/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceMenu;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipePropertySet;

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
        .range(1, 64)
        .sliderRange(1, 16)
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

    private final Setting<Boolean> autoClose = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-close")
        .defaultValue(false)
        .build()
    );

    public AutoSmelter() {
        super(Categories.World, "auto-smelter", "Automatically smelts items from your inventory");
    }

    private boolean fuelItemFilter(Item item) {
        if (!Utils.canUpdate()) return false;

        return mc.getConnection().fuelValues().fuelItems().contains(item);
    }

    private boolean smeltableItemFilter(Item item) {
        return mc.level != null && mc.level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(item.getDefaultInstance());
    }

    public void tick(AbstractFurnaceMenu c) {
        // Limit actions to happen every n ticks
        if (mc.player.tickCount % 10 == 0) return;

        // Check for fuel
        checkFuel(c);

        // Take the smelted results
        takeResults(c);

        // Insert new items
        insertItems(c);

        if (autoClose.get()) mc.setScreen(null);
    }

    private void insertItems(AbstractFurnaceMenu c) {
        ItemStack inputItemStack = c.slots.getFirst().getItem();
        if (!inputItemStack.isEmpty()) return;

        int slot = -1;

        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getItem();
            if (!((IAbstractFurnaceMenu) c).meteor$canSmelt(item)) continue;
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
        c.slots.getFirst().getItem().isEmpty();
    }

    private void checkFuel(AbstractFurnaceMenu c) {
        ItemStack fuelStack = c.slots.get(1).getItem();

        if (c.getLitProgress() > 0) return;
        if (!fuelStack.isEmpty()) return;

        int slot = -1;
        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getItem();
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

        if (slot == -1) return;

        ItemStack sourceStack = c.slots.get(slot).getItem();
        int moveCount = Math.min(fuelItemsPerRefill.get(), Math.min(sourceStack.getCount(), c.slots.get(1).getMaxStackSize(sourceStack)));

        if (moveCount <= 0) return;

        moveFuelItems(c, slot, moveCount);
    }

    private void moveFuelItems(AbstractFurnaceMenu c, int fromId, int amount) {
        if (amount <= 0 || mc.player == null || mc.gameMode == null) return;
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;

        mc.gameMode.handleContainerInput(c.containerId, fromId, 0, ContainerInput.PICKUP, mc.player);

        for (int i = 0; i < amount; i++) {
            if (mc.player.containerMenu.getCarried().isEmpty()) break;
            mc.gameMode.handleContainerInput(c.containerId, 1, 1, ContainerInput.PICKUP, mc.player);
        }

        if (!mc.player.containerMenu.getCarried().isEmpty()) {
            mc.gameMode.handleContainerInput(c.containerId, fromId, 0, ContainerInput.PICKUP, mc.player);
        }

        c.slots.get(1).getItem().isEmpty();
    }

    private void takeResults(AbstractFurnaceMenu c) {
        ItemStack resultStack = c.slots.get(2).getItem();
        if (resultStack.isEmpty()) return;

        InvUtils.shiftClick().slotId(2);

        if (!resultStack.isEmpty()) {
            error("Your inventory is full. Disabling.");
            toggle();
        }
    }
}
