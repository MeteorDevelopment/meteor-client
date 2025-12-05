/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.List;

public class BlockEntityTypeListSettingScreen extends CollectionListSettingScreen<BlockEntityType<?>> {
    public BlockEntityTypeListSettingScreen(GuiTheme theme, Setting<List<BlockEntityType<?>>> setting) {
        super(theme, "Select Block Entities", setting, setting.get(), Registries.BLOCK_ENTITY_TYPE);
    }

    @Override
    protected WWidget getValueWidget(BlockEntityType<?> value) {
        String name = Registries.BLOCK_ENTITY_TYPE.getId(value).toString();
        return theme.itemWithLabel(Items.BARRIER.getDefaultStack(), name);
    }

    @Override
    protected String[] getValueNames(BlockEntityType<?> value) {
        return new String[]{Registries.BLOCK_ENTITY_TYPE.getId(value).toString()};
    }
}
