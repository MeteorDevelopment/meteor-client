/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;

import java.util.List;

public class EnchantmentListSettingScreen extends LeftRightListSettingScreen<Enchantment> {
    public EnchantmentListSettingScreen(GuiTheme theme, Setting<List<Enchantment>> setting) {
        super(theme, "Select Enchantments", setting, setting.get(), Registries.ENCHANTMENT);
    }

    @Override
    protected WWidget getValueWidget(Enchantment value) {
        return theme.label(getValueName(value));
    }

    @Override
    protected String getValueName(Enchantment value) {
        return Names.get(value);
    }
}
