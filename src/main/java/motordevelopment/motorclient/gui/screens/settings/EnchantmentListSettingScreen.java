/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens.settings;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.widgets.WWidget;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.utils.misc.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.Set;

public class EnchantmentListSettingScreen extends DynamicRegistryListSettingScreen<Enchantment> {
    public EnchantmentListSettingScreen(GuiTheme theme, Setting<Set<RegistryKey<Enchantment>>> setting) {
        super(theme, "Select Enchantments", setting, setting.get(), RegistryKeys.ENCHANTMENT);
    }

    @Override
    protected WWidget getValueWidget(RegistryKey<Enchantment> value) {
        return theme.label(getValueName(value));
    }

    @Override
    protected String getValueName(RegistryKey<Enchantment> value) {
        return Names.get(value);
    }
}
