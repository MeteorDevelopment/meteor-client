/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;
import java.util.Map;

public class StorageBlockListSettingScreen extends RegistryListSettingScreen<BlockEntityType<?>> {
    private static final Map<BlockEntityType<?>, BlockEntityTypeInfo> BLOCK_ENTITY_TYPE_INFO_MAP = new Object2ObjectOpenHashMap<>();
    private static final BlockEntityTypeInfo UNKNOWN = new BlockEntityTypeInfo(Items.BARRIER, "Unknown");

    static {
        // Map of storage blocks
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.BARREL, new BlockEntityTypeInfo(Items.BARREL, "Barrel"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.BLAST_FURNACE, new BlockEntityTypeInfo(Items.BLAST_FURNACE, "Blast Furnace"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.BREWING_STAND, new BlockEntityTypeInfo(Items.BREWING_STAND, "Brewing Stand"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.CAMPFIRE, new BlockEntityTypeInfo(Items.CAMPFIRE, "Campfire"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.CHEST, new BlockEntityTypeInfo(Items.CHEST, "Chest"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.CHISELED_BOOKSHELF, new BlockEntityTypeInfo(Items.CHISELED_BOOKSHELF, "Chiseled Bookshelf"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.CRAFTER, new BlockEntityTypeInfo(Items.CRAFTER, "Crafter"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.DISPENSER, new BlockEntityTypeInfo(Items.DISPENSER, "Dispenser"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.DECORATED_POT, new BlockEntityTypeInfo(Items.DECORATED_POT, "Decorated Pot"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.DROPPER, new BlockEntityTypeInfo(Items.DROPPER, "Dropper"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.ENDER_CHEST, new BlockEntityTypeInfo(Items.ENDER_CHEST, "Ender Chest"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.FURNACE, new BlockEntityTypeInfo(Items.FURNACE, "Furnace"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.HOPPER, new BlockEntityTypeInfo(Items.HOPPER, "Hopper"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.SHULKER_BOX, new BlockEntityTypeInfo(Items.SHULKER_BOX, "Shulker Box"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.SMOKER, new BlockEntityTypeInfo(Items.SMOKER, "Smoker"));
        BLOCK_ENTITY_TYPE_INFO_MAP.put(BlockEntityType.TRAPPED_CHEST, new BlockEntityTypeInfo(Items.TRAPPED_CHEST, "Trapped Chest"));
    }

    public StorageBlockListSettingScreen(GuiTheme theme, Setting<List<BlockEntityType<?>>> setting) {
        super(theme, "Select Storage Blocks", setting, setting.get(), StorageBlockListSetting.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(BlockEntityType<?> value) {
        Item item = BLOCK_ENTITY_TYPE_INFO_MAP.getOrDefault(value, UNKNOWN).item();
        return theme.itemWithLabel(item.getDefaultStack(), getValueName(value));
    }

    @Override
    protected String getValueName(BlockEntityType<?> value) {
        return BLOCK_ENTITY_TYPE_INFO_MAP.getOrDefault(value, UNKNOWN).name();
    }

    private record BlockEntityTypeInfo(Item item, String name) {}
}
