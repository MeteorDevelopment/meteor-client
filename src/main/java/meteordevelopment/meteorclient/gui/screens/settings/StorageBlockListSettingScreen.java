/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import net.fabricmc.loader.impl.util.StringUtil;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StorageBlockListSettingScreen extends LeftRightListSettingScreen<BlockEntityType<?>> {
    private static final Map<BlockEntityType<?>, Pair<String, Item>> STORAGE_BLOCK_ENTITY_MAP = new HashMap<>();

    static {
        for (BlockEntityType<?> block : StorageBlockListSetting.STORAGE_BLOCKS) {
            try {
                Field nameField = findFieldObject(BlockEntityType.class, field -> {
                    try {
                        return field.getType() == BlockEntityType.class && field.get(null) == block;
                    } catch (IllegalAccessException ignored) {}
                    return false;
                });
                if (nameField == null) continue;
                Field itemField = findFieldObject(Items.class, field -> {
                    if (field.getType() == Item.class) return field.getName().equals(nameField.getName());
                    return false;
                });
                if (itemField == null) continue;
                String displayName = Arrays.stream(nameField.getName().toLowerCase().split("_")).map(StringUtil::capitalize).collect(Collectors.joining(" "));
                STORAGE_BLOCK_ENTITY_MAP.put(block, new Pair<>(displayName, (Item) itemField.get(null)));
            } catch (IllegalAccessException ignored) {}
        }
    }

    private static Field findFieldObject(Class<?> clazz, Predicate<Field> condition) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) if (condition.test(field)) return field;
        return null;
    }

    public StorageBlockListSettingScreen(GuiTheme theme, Setting<List<BlockEntityType<?>>> setting) {
        super(theme, "Select Storage Blocks", setting, setting.get(), StorageBlockListSetting.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(BlockEntityType<?> value) {
        Item item = Items.BARRIER;
        if (STORAGE_BLOCK_ENTITY_MAP.containsKey(value)) {
            item = STORAGE_BLOCK_ENTITY_MAP.get(value).getRight();
        }
        return theme.itemWithLabel(item.getDefaultStack(), getValueName(value));
    }

    @Override
    protected String getValueName(BlockEntityType<?> value) {
        if (STORAGE_BLOCK_ENTITY_MAP.containsKey(value)) {
            return STORAGE_BLOCK_ENTITY_MAP.get(value).getLeft();
        }
        return "Unknown";
    }
}
