/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.events.game;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class GetTooltipEvent {
    private static final GetTooltipEvent INSTANCE = new GetTooltipEvent();

    public ItemStack itemStack;
    public List<Text> list;

    public static GetTooltipEvent get(ItemStack itemStack, List<Text> list) {
        INSTANCE.itemStack = itemStack;
        INSTANCE.list = list;
        return INSTANCE;
    }
}
