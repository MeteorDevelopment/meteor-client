/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.entity.player;

import net.minecraft.item.ItemStack;

public class PickItemsEvent {
    private static final PickItemsEvent INSTANCE = new PickItemsEvent();

    public ItemStack itemStack;
    public int count;

    public static PickItemsEvent get(ItemStack itemStack, int count) {
        INSTANCE.itemStack = itemStack;
        INSTANCE.count = count;
        return INSTANCE;
    }
}
