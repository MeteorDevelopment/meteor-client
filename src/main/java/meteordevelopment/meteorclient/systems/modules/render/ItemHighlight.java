/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemHighlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("items")
            .description("Items to highlight.")
            .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("The color to highlight the items with.")
            .defaultValue(new SettingColor(225, 25, 255, 50))
            .build()
    );

    public ItemHighlight() {
        super(Categories.Render, "item-highlight", "Highlights selected items when in guis");
    }

    public int getColor(ItemStack stack) {
        if (items.get().contains(stack.getItem()) && isActive()) return color.get().getPacked();
        return -1;
    }
}
