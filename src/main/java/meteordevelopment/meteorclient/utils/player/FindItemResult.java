/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.player;

import net.minecraft.world.InteractionHand;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @param slot  The slot index
 * @param count The number of items in the slot
 */
public record FindItemResult(int slot, int count) {
    public boolean found() {
        return slot != -1;
    }

    public InteractionHand getHand() {
        if (slot == SlotUtils.OFFHAND) return InteractionHand.OFF_HAND;
        if (slot == mc.player.getInventory().getSelectedSlot()) return InteractionHand.MAIN_HAND;
        return null;
    }

    public boolean isMainHand() {
        return getHand() == InteractionHand.MAIN_HAND;
    }

    public boolean isOffhand() {
        return getHand() == InteractionHand.OFF_HAND;
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
