package minegame159.meteorclient.modules.misc;

import minegame159.meteorclient.mixininterface.IAbstractFurnaceContainer;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.InvUtils;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.container.AbstractFurnaceContainer;
import net.minecraft.container.SlotActionType;

public class AutoSmelter extends ToggleModule {
    private int step;
    private boolean first;
    private int timer;
    private boolean waitingForItemsToSmelt;

    public AutoSmelter() {
        super(Category.Misc, "auto-smelter", "Automatically smelts all smeltable items in your inventory.");
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

    public void tick(AbstractFurnaceContainer c) {
        timer++;

        // When furnace is opened
        if (!first) {
            first = true;

            step = 0;
            timer = 0;
        }

        // Check fuel
        if (checkFuel(c)) return;

        // Wait for smelting to complete
        if (c.getCookProgress() != 0 || timer < 5) return;

        if (step == 0) {
            // Take smelted results
            if (takeResults(c)) return;

            step++;
            timer = 0;
        } else if (step == 1) {
            // Wait for the items to smelt
            if (waitingForItemsToSmelt) {
                if (c.slots.get(0).getStack().isEmpty()) {
                    step = 0;
                    timer = 0;
                    waitingForItemsToSmelt = false;
                }
                return;
            }

            // Insert items
            if (insertItems(c)) return;

            waitingForItemsToSmelt = true;
        }
    }

    private boolean insertItems(AbstractFurnaceContainer c) {
        if (!c.slots.get(0).getStack().isEmpty()) return true;

        int slot = -1;

        for (int i = 3; i < c.slots.size(); i++) {
            if (((IAbstractFurnaceContainer) c).isSmeltableI(c.slots.get(i).getStack())) {
                slot = i;
                break;
            }
        }

        if (slot == -1) {
            Chat.warning(this, "Disabled because you don't have any items to smelt in your inventory.");
            toggle();
            return true;
        }

        InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
        InvUtils.clickSlot(0, 0, SlotActionType.PICKUP);

        return false;
    }

    private boolean checkFuel(AbstractFurnaceContainer c) {
        if (c.getFuelProgress() <= 1 && !((IAbstractFurnaceContainer) c).isFuelI(c.slots.get(1).getStack())) {
            if (!c.slots.get(1).getStack().isEmpty()) {
                InvUtils.clickSlot(1, 0, SlotActionType.QUICK_MOVE);

                if (!c.slots.get(1).getStack().isEmpty()) {
                    Chat.warning(this, "Disabled because your inventory is full.");
                    toggle();
                    return true;
                }
            }

            int slot = -1;
            for (int i = 3; i < c.slots.size(); i++) {
                if (((IAbstractFurnaceContainer) c).isFuelI(c.slots.get(i).getStack())) {
                    slot = i;
                    break;
                }
            }

            if (slot == -1) {
                Chat.warning(this, "Disabled because you don't have any fuel in your inventory.");
                toggle();
                return true;
            }

            InvUtils.clickSlot(slot, 0, SlotActionType.PICKUP);
            InvUtils.clickSlot(1, 0, SlotActionType.PICKUP);
        }

        return false;
    }

    private boolean takeResults(AbstractFurnaceContainer c) {
        InvUtils.clickSlot(2, 0, SlotActionType.QUICK_MOVE);

        if (!c.slots.get(2).getStack().isEmpty()) {
            Chat.warning(this, "Disabled because your inventory is full.");
            toggle();
            return true;
        }

        return false;
    }
}
