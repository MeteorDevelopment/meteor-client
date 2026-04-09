/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceMenu;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
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

        InvUtils.move().fromId(slot).toId(0);
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

        InvUtils.move().fromId(slot).toId(1);
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
