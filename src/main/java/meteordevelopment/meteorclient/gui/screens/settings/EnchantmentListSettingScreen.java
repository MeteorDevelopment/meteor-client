/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.DynamicRegistryListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

public class EnchantmentListSettingScreen extends DynamicRegistryListSettingScreen<Enchantment> {
    public EnchantmentListSettingScreen(GuiTheme theme, Setting<Set<ResourceKey<Enchantment>>> setting) {
        super(theme, "Select Enchantments", setting, setting.get(), Registries.ENCHANTMENT);
    }

    @Override
    protected WWidget getValueWidget(ResourceKey<Enchantment> value) {
        return theme.label(Names.get(value));
    }

    @Override
    protected String[] getValueNames(ResourceKey<Enchantment> value) {
        return new String[]{
            Names.get(value),
            value.identifier().toString()
        };
    }
}
