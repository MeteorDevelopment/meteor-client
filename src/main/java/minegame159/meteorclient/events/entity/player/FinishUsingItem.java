/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.entity.player;

import net.minecraft.item.ItemStack;

public class FinishUsingItem {
    private static final FinishUsingItem INSTANCE = new FinishUsingItem();

    public ItemStack itemStack;

    public static FinishUsingItem get(ItemStack itemStack) {
        INSTANCE.itemStack = itemStack;
        return INSTANCE;
    }
}
