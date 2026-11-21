/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

public class ItemListSettingScreen extends WindowScreen {
    private final ItemListSetting setting;

    private WVerticalList list;
    private final WTextBox filter;

    private String filterText = "";

    private final EnumMap<ItemCategory, CategoryState> categories = new EnumMap<>(ItemCategory.class);

    public ItemListSettingScreen(GuiTheme theme, ItemListSetting setting) {
        super(theme, "Select Items");
        this.setting = setting;

        filter = super.add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            list.clear();
            initWidgets();
        };

        list = super.add(theme.verticalList()).expandX().widget();

        for (ItemCategory cat : ItemCategory.values()) {
            categories.put(cat, new CategoryState(cat));
        }
    }

    @Override
    public <W extends WWidget> Cell<W> add(W widget) {
        return list.add(widget);
    }

    @Override
    public void initWidgets() {
        for (CategoryState state : categories.values()) {
            state.count = 0;
            state.selectedItems.clear();
        }

        for (Item item : setting.get()) {
            if (item == Items.AIR) continue;
            if (setting.filter != null) {
                try {
                    if (!setting.filter.test(item)) continue;
                } catch (Exception ignored) {}
            }
            categories.get(getCategory(item)).count++;
        }

        boolean first = categories.get(ItemCategory.BLOCKS).section == null;

        for (CategoryState state : categories.values()) {
            state.checkbox = theme.checkbox(state.count > 0);
            state.section = theme.section(state.category.displayName, state.section != null && state.section.isExpanded(), state.checkbox);
            state.checkbox.action = () -> tableChecked(state);
            state.cell = add(state.section).expandX();
            state.table = state.section.add(theme.table()).expandX().widget();
        }

        Consumer<Item> itemForEach = item -> {
            if (item == Items.AIR) return;
            if (setting.filter != null) {
                try {
                    if (!setting.filter.test(item)) return;
                } catch (Exception ignored) {}
            }

            CategoryState state = categories.get(getCategory(item));
            state.selectedItems.add(item);
            addItem(state, item);
        };

        if (filterText.isEmpty()) {
            BuiltInRegistries.ITEM.forEach(itemForEach);
        } else {
            List<Pair<Item, Integer>> items = new ArrayList<>();
            BuiltInRegistries.ITEM.forEach(item -> {
                int words = Utils.searchInWords(Names.get(item), filterText);
                int diff = Utils.searchLevenshteinDefault(Names.get(item), filterText, false);

                if (words > 0 || diff < Names.get(item).length() / 2) items.add(Pair.of(item, -diff));
            });
            items.sort(Comparator.comparingInt(value -> -value.getSecond()));
            for (Pair<Item, Integer> pair : items) itemForEach.accept(pair.getFirst());
        }

        for (CategoryState state : categories.values()) {
            if (state.table.cells.isEmpty()) list.cells.remove(state.cell);
        }

        if (first) {
            int totalCount = categories.values().stream().mapToInt(s -> s.count).sum() / 2;
            boolean expand = totalCount <= 20;

            for (CategoryState state : categories.values()) {
                if (!state.table.cells.isEmpty()) {
                    state.section.setExpanded(expand);
                }
            }
        }
    }

    private void tableChecked(CategoryState state) {
        boolean changed = false;

        for (Item item : state.selectedItems) {
            if (state.checkbox.checked) {
                setting.get().add(item);
                changed = true;
            } else {
                if (setting.get().remove(item)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            list.clear();
            initWidgets();
            setting.onChanged();
        }
    }

    private void addItem(CategoryState state, Item item) {
        state.table.add(theme.item(DisplayItemUtils.toStack(item)));
        state.table.add(theme.label(Names.get(item)));

        WCheckbox a = state.table.add(theme.checkbox(setting.get().contains(item))).expandCellX().right().widget();
        a.action = () -> {
            if (a.checked) {
                setting.get().add(item);
                if (state.count == 0) state.checkbox.checked = true;
                state.count++;
            } else {
                if (setting.get().remove(item)) {
                    state.count--;
                    if (state.count == 0) state.checkbox.checked = false;
                }
            }

            setting.onChanged();
        };

        state.table.row();
    }

    private enum ItemCategory {
        BLOCKS("Blocks"),
        TOOLS("Tools"),
        WEAPONS("Weapons"),
        ARMOR("Armor"),
        FOOD("Food"),
        POTIONS("Potions"),
        PROJECTILES("Projectiles"),
        SPAWN_EGGS("Spawn Eggs"),
        OTHER("Other");

        final String displayName;

        ItemCategory(String displayName) {
            this.displayName = displayName;
        }
    }

    private static class CategoryState {
        final ItemCategory category;
        final List<Item> selectedItems = new ArrayList<>();
        WSection section;
        WTable table;
        WCheckbox checkbox;
        int count;
        Cell<WSection> cell;

        CategoryState(ItemCategory category) {
            this.category = category;
        }
    }

    private static ItemCategory getCategory(Item item) {
        // The ordering of these checks is important! If you change the order double check that items are in expected categories.

        if (item instanceof BowItem
            || item instanceof CrossbowItem
            || item instanceof TridentItem
            || item instanceof MaceItem
        ) return ItemCategory.WEAPONS;
        if (item instanceof ArrowItem
            || item instanceof SnowballItem
            || item instanceof EggItem
            || item instanceof EnderpearlItem
            || item instanceof ExperienceBottleItem
            || item instanceof WindChargeItem
            || item instanceof FireworkRocketItem
        ) return ItemCategory.PROJECTILES;
        if (item instanceof SpawnEggItem) return ItemCategory.SPAWN_EGGS;

        // Food check must be before block check so carrots and other food items which can also be planted don't get placed with blocks
        try {
            if (Utils.isFood(item)) return ItemCategory.FOOD;
        } catch (Exception ignored) {}

        // Block check should come before Armor check or carpets will end up Armor category
        if (item instanceof BlockItem) return ItemCategory.BLOCKS;

        try {
            if (item.getDefaultInstance().is(ItemTags.SWORDS) || item.getDefaultInstance().is(ItemTags.SPEARS)) return ItemCategory.WEAPONS;
            if (item.components().has(DataComponents.EQUIPPABLE)) return ItemCategory.ARMOR;
            if (item.components().has(DataComponents.TOOL)) return ItemCategory.TOOLS;
            if (item.components().has(DataComponents.POTION_CONTENTS)) return ItemCategory.POTIONS;
            if (item.components().has(DataComponents.ENTITY_DATA)) return ItemCategory.SPAWN_EGGS;
        } catch (Exception ignored) {}


        return ItemCategory.OTHER;
    }
}
