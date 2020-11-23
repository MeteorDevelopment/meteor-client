/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.PotionSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.MyPotion;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class AutoBrewer extends ToggleModule {
    public enum Modifier {
        None,
        Splash,
        Lingering
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<MyPotion> potion = sgGeneral.add(new PotionSetting.Builder()
            .name("potion")
            .description("Potion to brew.")
            .defaultValue(MyPotion.Strength)
            .build()
    );

    private final Setting<Modifier> modifier = sgGeneral.add(new EnumSetting.Builder<Modifier>()
            .name("modifier")
            .description("Modifier.")
            .defaultValue(Modifier.None).build()
    );

    private int ingredientI;
    private boolean first;
    private int timer;

    public AutoBrewer() {
        super(Category.Misc, "auto-brewer", "Automatically brews potions.");
    }

    @Override
    public void onActivate() {
        first = false;
    }

    public void onBrewingStandClose() {
        first = false;
    }

    public void tick(BrewingStandScreenHandler c) {
        timer++;

        // When brewing stand is opened
        if (!first) {
            first = true;

            ingredientI = -2;
            timer = 0;
        }

        // Wait for brewing to complete
        if (c.getBrewTime() != 0 || timer < 5) return;

        if (ingredientI == -2) {
            // Take bottles
            if (takePotions(c)) return;
            ingredientI++;
            timer = 0;
        } else if (ingredientI == -1) {
            // Insert water bottles
            if (insertWaterBottles(c)) return;
            ingredientI++;
            timer = 0;
        } else if (ingredientI < potion.get().ingredients.length) {
            // Check fuel and insert ingredient
            if (checkFuel(c)) return;
            if (insertIngredient(c, potion.get().ingredients[ingredientI])) return;
            ingredientI++;
            timer = 0;
        } else if (ingredientI == potion.get().ingredients.length) {
            // Apply modifier
            if (applyModifier(c)) return;
            ingredientI++;
            timer = 0;
        } else {
            // Reset loop
            ingredientI = -2;
            timer = 0;
        }
    }

    private boolean applyModifier(BrewingStandScreenHandler c) {
        if (modifier.get() != Modifier.None) {
            Item item;
            if (modifier.get() == Modifier.Splash) item = Items.GUNPOWDER;
            else item = Items.DRAGON_BREATH;

            int slot = -1;

            for (int slotI = 5; slotI < c.slots.size(); slotI++) {
                if (c.slots.get(slotI).getStack().getItem() == item) {
                    slot = slotI;
                    break;
                }
            }

            if (slot == -1) {
                Chat.warning(this, "Disabled because you don't have any %s left in your inventory.", item.getName().getString());
                toggle();
                return true;
            }

            moveOneItem(c, slot, 3);
        }

        return false;
    }

    private boolean insertIngredient(BrewingStandScreenHandler c, Item ingredient) {
        int slot = -1;

        for (int slotI = 5; slotI < c.slots.size(); slotI++) {
            if (c.slots.get(slotI).getStack().getItem() == ingredient) {
                slot = slotI;
                break;
            }
        }

        if (slot == -1) {
            Chat.warning(this, "Disabled because you don't have any %s left in your inventory.", ingredient.getName().getString());
            toggle();
            return true;
        }

        moveOneItem(c, slot, 3);

        return false;
    }

    private boolean checkFuel(BrewingStandScreenHandler c) {
        if (c.getFuel() == 0) {
            int slot = -1;

            for (int slotI = 5; slotI < c.slots.size(); slotI++) {
                if (c.slots.get(slotI).getStack().getItem() == Items.BLAZE_POWDER) {
                    slot = slotI;
                    break;
                }
            }

            if (slot == -1) {
                Chat.warning(this, "Disabled because you don't have any blaze powder (as fuel) left in your inventory.");
                toggle();
                return true;
            }

            moveOneItem(c, slot, 4);
        }

        return false;
    }

    private void moveOneItem(BrewingStandScreenHandler c, int from, int to) {
        InvUtils.clickSlot(from, 0, SlotActionType.PICKUP);
        InvUtils.clickSlot(to, 1, SlotActionType.PICKUP);
        InvUtils.clickSlot(from, 0, SlotActionType.PICKUP);
    }

    private boolean insertWaterBottles(BrewingStandScreenHandler c) {
        for (int i = 0; i < 3; i++) {
            int slot = -1;

            for (int slotI = 5; slotI < c.slots.size(); slotI++) {
                if (c.slots.get(slotI).getStack().getItem() == Items.POTION) {
                    Potion potion = PotionUtil.getPotion(c.slots.get(slotI).getStack());
                    if (potion == Potions.WATER) {
                        slot = slotI;
                        break;
                    }
                }
            }

            if (slot == -1) {
                Chat.warning(this, "Disabled because you don't have any water bottles left in your inventory.");
                toggle();
                return true;
            }

            InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(i, 0, SlotActionType.PICKUP);
        }

        return false;
    }

    private boolean takePotions(BrewingStandScreenHandler c) {
        for (int i = 0; i < 3; i++) {
            InvUtils.clickSlot(i, 0, SlotActionType.QUICK_MOVE);

            if (!c.slots.get(i).getStack().isEmpty()) {
                Chat.warning(this, "Disabled because your inventory is full.");
                toggle();
                return true;
            }
        }

        return false;
    }
}
