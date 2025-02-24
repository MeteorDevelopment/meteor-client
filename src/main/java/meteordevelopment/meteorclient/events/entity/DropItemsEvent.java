/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.item.ItemStack;

public class DropItemsEvent extends Cancellable {
    private static final DropItemsEvent INSTANCE = new DropItemsEvent();

    public ItemStack itemStack;
    public int slotId;

    public static DropItemsEvent get(ItemStack itemStack) {
        return get(itemStack, -999);
    }

    public static DropItemsEvent get(ItemStack itemStack, int slotId) {
        INSTANCE.setCancelled(false);
        INSTANCE.itemStack = itemStack;
        INSTANCE.slotId = slotId;
        return INSTANCE;
    }
}
