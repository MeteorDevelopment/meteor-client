/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.player;

import net.minecraft.util.Hand;

import static minegame159.meteorclient.utils.Utils.mc;

public class FindItemResult {
    public final int slot, count;

    public FindItemResult(int slot, int count) {
        this.slot = slot;
        this.count = count;
    }

    public int getSlot() {
        return slot;
    }

    public int getCount() {
        return count;
    }

    public boolean found() {
        return slot != -1;
    }

    public Hand getHand() {
        if (slot == SlotUtils.OFFHAND) return Hand.OFF_HAND;
        else if (slot == mc.player.getInventory().selectedSlot) return Hand.MAIN_HAND;
        return null;
    }

    public boolean isMainHand() {
        return getHand() == Hand.MAIN_HAND;
    }

    public boolean isOffhand() {
        return getHand() == Hand.OFF_HAND;
    }

    public boolean isHotbar() {
        return slot >= SlotUtils.HOTBAR_START && slot <= SlotUtils.HOTBAR_END;
    }

    public boolean isMain() {
        return slot >= SlotUtils.MAIN_START && slot <= SlotUtils.MAIN_END;
    }

    public boolean isArmor() {
        return slot >= SlotUtils.ARMOR_START && slot <= SlotUtils.ARMOR_END;
    }
}
