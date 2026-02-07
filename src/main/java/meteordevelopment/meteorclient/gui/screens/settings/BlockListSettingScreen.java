/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

import java.util.function.Predicate;

public class BlockListSettingScreen extends CollectionListSettingScreen<Block> {
    public BlockListSettingScreen(GuiTheme theme, BlockListSetting setting) {
        super(theme, "Select Blocks", setting, setting.get(), Registries.BLOCK);
    }

    @Override
    protected boolean includeValue(Block value) {
        Predicate<Block> filter = ((BlockListSetting) setting).filter;

        if (filter == null) return value != Blocks.AIR;
        return filter.test(value);
    }

    @Override
    protected WWidget getValueWidget(Block block) {
        return theme.blockWithLabel(block.getDefaultState());
    }

    @Override
    protected String[] getValueNames(Block value) {
        return new String[]{
            Names.get(value),
            Registries.BLOCK.getId(value).toString()
        };
    }
}
