/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.events.entity;

import minegame159.meteorclient.events.Cancellable;
import net.minecraft.item.ItemStack;

public class DropItemsEvent extends Cancellable {
    private static final DropItemsEvent INSTANCE = new DropItemsEvent();

    public ItemStack itemStack;

    public static DropItemsEvent get(ItemStack itemStack) {
        INSTANCE.setCancelled(false);
        INSTANCE.itemStack = itemStack;
        return INSTANCE;
    }
}
