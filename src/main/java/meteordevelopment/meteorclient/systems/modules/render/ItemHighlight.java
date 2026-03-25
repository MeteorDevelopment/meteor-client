/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.ColorListSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ItemHighlight extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items to highlight.")
        .build()
    );

    public final Setting<List<SettingColor>> colors = sgGeneral.add(new ColorListSetting.Builder()
        .name("colors")
        .description("Colors used for each individual item.")
        .defaultValue(List.of(new SettingColor(225,25,255,50)))
        .build()
    );

    public int index;

    public ItemHighlight() {
        super(Categories.Render, "item-highlight", "Highlights selected items when in guis");
    }

    public int getColor(ItemStack stack)
    {
        if (isActive() && stack != null && colors.get().size() == items.get().size())
        {
            index = items.get().indexOf(stack.getItem());

            if (index != -1)
                return colors.get().get(index).getPacked();
        }
        return -1;
    }
}
