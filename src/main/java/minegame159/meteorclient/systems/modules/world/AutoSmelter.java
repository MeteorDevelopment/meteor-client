/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.world;

import minegame159.meteorclient.mixin.AbstractFurnaceScreenHandlerAccessor;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import net.minecraft.screen.AbstractFurnaceScreenHandler;

public class AutoSmelter extends Module {
    private int step;
    private boolean first;
    private int timer;
    private boolean waitingForItemsToSmelt;

    public AutoSmelter() {
        super(Categories.World, "auto-smelter", "Automatically smelts all items in your inventory that can be smelted.");
    }

    @Override
    public void onActivate() {
        first = true;
        waitingForItemsToSmelt = false;
    }

    public void onFurnaceClose() {
        first = true;
        waitingForItemsToSmelt = false;
    }

    public void tick(AbstractFurnaceScreenHandler c) {
        timer++;

        // When the furnace is opened.
        if (!first) {
            first = true;

            step = 0;
            timer = 0;
        }

        // Check for fuel.
        if (checkFuel(c)) return;

        // Wait for the smelting to be complete.
        if (c.getCookProgress() != 0 || timer < 5) return;

        if (step == 0) {
            // Take the smelted results.
            if (takeResults(c)) return;

            step++;
            timer = 0;
        } else if (step == 1) {
            // Wait for the items to smelt.
            if (waitingForItemsToSmelt) {
                if (c.slots.get(0).getStack().isEmpty()) {
                    step = 0;
                    timer = 0;
                    waitingForItemsToSmelt = false;
                }
                return;
            }

            // Insert items.
            if (insertItems(c)) return;

            waitingForItemsToSmelt = true;
        }
    }

    private boolean insertItems(AbstractFurnaceScreenHandler c) {
        if (!c.slots.get(0).getStack().isEmpty()) return true;

        int slot = -1;

        for (int i = 3; i < c.slots.size(); i++) {
            if (((AbstractFurnaceScreenHandlerAccessor) c).isSmeltable(c.slots.get(i).getStack())) {
                slot = i;
                break;
            }
        }

        if (slot == -1) {
            ChatUtils.moduleError(this, "You do not have any items in your inventory that can be smelted... disabling.");
            toggle();
            return true;
        }

        InvUtils.move().fromId(slot).toId(0);

        return false;
    }

    private boolean checkFuel(AbstractFurnaceScreenHandler c) {
        if (c.getFuelProgress() <= 1 && !((AbstractFurnaceScreenHandlerAccessor) c).isFuel(c.slots.get(1).getStack())) {
            if (!c.slots.get(1).getStack().isEmpty()) {
                InvUtils.quickMove().slotId(1);

                if (!c.slots.get(1).getStack().isEmpty()) {
                    ChatUtils.moduleError(this, "Your inventory is currently full... disabling.");
                    toggle();
                    return true;
                }
            }

            int slot = -1;
            for (int i = 3; i < c.slots.size(); i++) {
                if (((AbstractFurnaceScreenHandlerAccessor) c).isFuel(c.slots.get(i).getStack())) {
                    slot = i;
                    break;
                }
            }

            if (slot == -1) {
                ChatUtils.moduleError(this, "You do not have any fuel in your inventory... disabling.");
                toggle();
                return true;
            }

            InvUtils.move().fromId(slot).toId(1);
        }

        return false;
    }

    private boolean takeResults(AbstractFurnaceScreenHandler c) {
        InvUtils.quickMove().slotId(2);

        if (!c.slots.get(2).getStack().isEmpty()) {
            ChatUtils.moduleError(this, "Your inventory is full... disabling.");
            toggle();
            return true;
        }

        return false;
    }
}
