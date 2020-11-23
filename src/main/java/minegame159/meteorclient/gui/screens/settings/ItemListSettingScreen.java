/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.widgets.WItemWithLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;

import java.util.List;

public class ItemListSettingScreen extends LeftRightListSettingScreen<Item> {
    public ItemListSettingScreen(Setting<List<Item>> setting) {
        super("Select items", setting, Registry.ITEM);
    }

    @Override
    protected boolean includeValue(Item value) {
        return value != Items.AIR;
    }

    @Override
    protected WWidget getValueWidget(Item value) {
        return new WItemWithLabel(value.getDefaultStack());
    }

    @Override
    protected String getValueName(Item value) {
        return value.getName().getString();
    }
}
