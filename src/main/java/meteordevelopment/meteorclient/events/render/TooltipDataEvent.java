/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.render;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;


public class TooltipDataEvent {
    private static final TooltipDataEvent INSTANCE = new TooltipDataEvent();

    public TooltipComponent tooltipData;
    public ItemStack itemStack;

    public static TooltipDataEvent get(ItemStack itemStack) {
        INSTANCE.tooltipData = null;
        INSTANCE.itemStack = itemStack;
        return INSTANCE;
    }
}
