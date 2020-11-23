/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.settings.Setting;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.registry.Registry;

import java.util.List;

public class EnchListSettingScreen extends LeftRightListSettingScreen<Enchantment> {
    public EnchListSettingScreen(Setting<List<Enchantment>> setting) {
        super("Select items", setting, Registry.ENCHANTMENT);
    }

    @Override
    protected WWidget getValueWidget(Enchantment value) {
        return new WLabel(value.getName(1).getString());
    }

    @Override
    protected String getValueName(Enchantment value) {
        return value.getName(1).getString();
    }
}
