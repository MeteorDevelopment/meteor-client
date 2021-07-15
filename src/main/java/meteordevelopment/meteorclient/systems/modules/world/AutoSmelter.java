/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.mixin.AbstractFurnaceScreenHandlerAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.AbstractFurnaceScreenHandler;

import java.util.Arrays;
import java.util.List;

public class AutoSmelter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> fuelItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("fuel-items")
        .description("Items to use as fuel")
        .defaultValue(Arrays.asList(Items.COAL, Items.CHARCOAL))
        .filter(x -> AbstractFurnaceBlockEntity.canUseAsFuel(x.getDefaultStack()))
        .build()
    );

    private final Setting<List<Item>> smeltableItems = sgGeneral.add(new ItemListSetting.Builder()
        .name("smeltable-items")
        .description("Items to smelt")
        .defaultValue(Arrays.asList(Items.IRON_ORE, Items.GOLD_ORE, Items.COPPER_ORE, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD))
        .filter(x -> mc.world.getRecipeManager() == null || mc.world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SimpleInventory(x.getDefaultStack()), mc.world).isPresent())
        .build()
    );

    private final Setting<Boolean> disableWhenOutOfItems = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-out-of-items")
        .description("Disable the module when you run out of items")
        .defaultValue(true)
        .build()
    );


    public AutoSmelter() {
        super(Categories.World, "auto-smelter", "Automatically smelts all items in your inventory that can be smelted.");
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

    private void insertItems(AbstractFurnaceScreenHandler c) {
        ItemStack inputItemStack = c.slots.get(0).getStack();
        if (!inputItemStack.isEmpty()) return;

        int slot = -1;

        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getStack();
            if (!((AbstractFurnaceScreenHandlerAccessor) c).isSmeltable(item)) continue;
            if (!smeltableItems.get().contains(item.getItem())) continue;
            slot = i;
            break;
        }

        if (disableWhenOutOfItems.get() && slot == -1) {
            error("You do not have any items in your inventory that can be smelted... disabling.");
            toggle();
            return;
        }

        InvUtils.move().fromId(slot).toId(0);
    }

    private void checkFuel(AbstractFurnaceScreenHandler c) {
        ItemStack fuelStack = c.slots.get(1).getStack();

        if (c.getFuelProgress() > 0) return;
        if (!fuelStack.isEmpty()) return;

        int slot = -1;
        for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getStack();
            if (!fuelItems.get().contains(item.getItem())) continue;
            if (!AbstractFurnaceBlockEntity.canUseAsFuel(item)) continue;
            slot = i;
            break;
        }

        if (disableWhenOutOfItems.get() && slot == -1) {
            error("You do not have any fuel in your inventory... disabling.");
            toggle();
            return;
        }

        InvUtils.move().fromId(slot).toId(1);
    }

    private void takeResults(AbstractFurnaceScreenHandler c) {
        ItemStack resultStack = c.slots.get(2).getStack();
        if (resultStack.isEmpty()) return;

        InvUtils.quickMove().slotId(2);

        if (!resultStack.isEmpty()) {
            error("Your inventory is full... disabling.");
            toggle();
        }
    }
}
