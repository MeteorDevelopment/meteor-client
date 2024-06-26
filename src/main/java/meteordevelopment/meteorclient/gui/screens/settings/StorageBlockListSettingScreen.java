/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class StorageBlockListSettingScreen extends LeftRightListSettingScreen<BlockEntityType<?>> {
    public StorageBlockListSettingScreen(GuiTheme theme, Setting<List<BlockEntityType<?>>> setting) {
        super(theme, "Select Storage Blocks", setting, setting.get(), StorageBlockListSetting.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(BlockEntityType<?> value) {
        Item item = Items.BARRIER;

        if (value == BlockEntityType.FURNACE) item = Items.FURNACE;
        else if (value == BlockEntityType.CHEST) item = Items.CHEST;
        else if (value == BlockEntityType.TRAPPED_CHEST) item = Items.TRAPPED_CHEST;
        else if (value == BlockEntityType.ENDER_CHEST) item = Items.ENDER_CHEST;
        else if (value == BlockEntityType.DISPENSER) item = Items.DISPENSER;
        else if (value == BlockEntityType.DROPPER) item = Items.DROPPER;
        else if (value == BlockEntityType.HOPPER) item = Items.HOPPER;
        else if (value == BlockEntityType.SHULKER_BOX) item = Items.SHULKER_BOX;
        else if (value == BlockEntityType.BARREL) item = Items.BARREL;
        else if (value == BlockEntityType.SMOKER) item = Items.SMOKER;
        else if (value == BlockEntityType.BLAST_FURNACE) item = Items.BLAST_FURNACE;

        return theme.itemWithLabel(item.getDefaultStack(), getValueName(value));
    }

    @Override
    protected String getValueName(BlockEntityType<?> value) {
        String name = "Unknown";

        if (value == BlockEntityType.FURNACE) name = "Furnace";
        else if (value == BlockEntityType.CHEST) name = "Chest";
        else if (value == BlockEntityType.TRAPPED_CHEST) name = "Trapped Chest";
        else if (value == BlockEntityType.ENDER_CHEST) name = "Ender Chest";
        else if (value == BlockEntityType.DISPENSER) name = "Dispenser";
        else if (value == BlockEntityType.DROPPER) name = "Dropper";
        else if (value == BlockEntityType.HOPPER) name = "Hopper";
        else if (value == BlockEntityType.SHULKER_BOX) name = "Shulker Box";
        else if (value == BlockEntityType.BARREL) name = "Barrel";
        else if (value == BlockEntityType.SMOKER) name = "Smoker";
        else if (value == BlockEntityType.BLAST_FURNACE) name = "Blast Furnace";

        return name;
    }
}
