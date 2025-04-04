/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.events.entity.player;

import net.minecraft.item.ItemStack;

public class FinishUsingItemEvent {
    private static final FinishUsingItemEvent INSTANCE = new FinishUsingItemEvent();

    public ItemStack itemStack;

    public static FinishUsingItemEvent get(ItemStack itemStack) {
        INSTANCE.itemStack = itemStack;
        return INSTANCE;
    }
}
