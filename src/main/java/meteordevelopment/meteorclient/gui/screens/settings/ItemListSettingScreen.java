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
import net.minecraft.util.Tuple;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class ItemListSettingScreen extends WindowScreen {
    private final ItemListSetting setting;

    private WVerticalList list;
    private final WTextBox filter;

    private String filterText = "";

    private WSection blocksSection, toolsSection, weaponsSection, armorSection, foodSection,
        potionsSection, projectilesSection, spawnEggsSection, otherSection;
    private WTable blocksT, toolsT, weaponsT, armorT, foodT,
        potionsT, projectilesT, spawnEggsT, otherT;

    int hasBlocks = 0, hasTools = 0, hasWeapons = 0, hasArmor = 0, hasFood = 0,
        hasPotions = 0, hasProjectiles = 0, hasSpawnEggs = 0, hasOther = 0;

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
    }

    @Override
    public <W extends WWidget> Cell<W> add(W widget) {
        return list.add(widget);
    }

    @Override
    public void initWidgets() {
        hasBlocks = hasTools = hasWeapons = hasArmor = hasFood = hasPotions = hasProjectiles = hasSpawnEggs = hasOther = 0;

        for (Item item : setting.get()) {
            if (item == Items.AIR) continue;
            if (setting.filter != null) {
                try {
                    if (!setting.filter.test(item)) continue;
                } catch (Exception ignored) {}
            }

            switch (getCategory(item)) {
                case BLOCKS -> hasBlocks++;
                case TOOLS -> hasTools++;
                case WEAPONS -> hasWeapons++;
                case ARMOR -> hasArmor++;
                case FOOD -> hasFood++;
                case POTIONS -> hasPotions++;
                case PROJECTILES -> hasProjectiles++;
                case SPAWN_EGGS -> hasSpawnEggs++;
                case OTHER -> hasOther++;
            }
        }

        boolean first = blocksSection == null;

        // Blocks
        List<Item> blocksItemsE = new ArrayList<>();
        WCheckbox blocksC = theme.checkbox(hasBlocks > 0);
        blocksSection = theme.section("Blocks", blocksSection != null && blocksSection.isExpanded(), blocksC);
        blocksC.action = () -> tableChecked(blocksItemsE, blocksC.checked);
        Cell<WSection> blocksCell = add(blocksSection).expandX();
        blocksT = blocksSection.add(theme.table()).expandX().widget();

        // Tools
        List<Item> toolsItemsE = new ArrayList<>();
        WCheckbox toolsC = theme.checkbox(hasTools > 0);
        toolsSection = theme.section("Tools", toolsSection != null && toolsSection.isExpanded(), toolsC);
        toolsC.action = () -> tableChecked(toolsItemsE, toolsC.checked);
        Cell<WSection> toolsCell = add(toolsSection).expandX();
        toolsT = toolsSection.add(theme.table()).expandX().widget();

        // Weapons
        List<Item> weaponsItemsE = new ArrayList<>();
        WCheckbox weaponsC = theme.checkbox(hasWeapons > 0);
        weaponsSection = theme.section("Weapons", weaponsSection != null && weaponsSection.isExpanded(), weaponsC);
        weaponsC.action = () -> tableChecked(weaponsItemsE, weaponsC.checked);
        Cell<WSection> weaponsCell = add(weaponsSection).expandX();
        weaponsT = weaponsSection.add(theme.table()).expandX().widget();

        // Armor
        List<Item> armorItemsE = new ArrayList<>();
        WCheckbox armorC = theme.checkbox(hasArmor > 0);
        armorSection = theme.section("Armor", armorSection != null && armorSection.isExpanded(), armorC);
        armorC.action = () -> tableChecked(armorItemsE, armorC.checked);
        Cell<WSection> armorCell = add(armorSection).expandX();
        armorT = armorSection.add(theme.table()).expandX().widget();

        // Food
        List<Item> foodItemsE = new ArrayList<>();
        WCheckbox foodC = theme.checkbox(hasFood > 0);
        foodSection = theme.section("Food", foodSection != null && foodSection.isExpanded(), foodC);
        foodC.action = () -> tableChecked(foodItemsE, foodC.checked);
        Cell<WSection> foodCell = add(foodSection).expandX();
        foodT = foodSection.add(theme.table()).expandX().widget();

        // Potions
        List<Item> potionsItemsE = new ArrayList<>();
        WCheckbox potionsC = theme.checkbox(hasPotions > 0);
        potionsSection = theme.section("Potions", potionsSection != null && potionsSection.isExpanded(), potionsC);
        potionsC.action = () -> tableChecked(potionsItemsE, potionsC.checked);
        Cell<WSection> potionsCell = add(potionsSection).expandX();
        potionsT = potionsSection.add(theme.table()).expandX().widget();

        // Projectiles
        List<Item> projectilesItemsE = new ArrayList<>();
        WCheckbox projectilesC = theme.checkbox(hasProjectiles > 0);
        projectilesSection = theme.section("Projectiles", projectilesSection != null && projectilesSection.isExpanded(), projectilesC);
        projectilesC.action = () -> tableChecked(projectilesItemsE, projectilesC.checked);
        Cell<WSection> projectilesCell = add(projectilesSection).expandX();
        projectilesT = projectilesSection.add(theme.table()).expandX().widget();

        // Spawn Eggs
        List<Item> spawnEggsItemsE = new ArrayList<>();
        WCheckbox spawnEggsC = theme.checkbox(hasSpawnEggs > 0);
        spawnEggsSection = theme.section("Spawn Eggs", spawnEggsSection != null && spawnEggsSection.isExpanded(), spawnEggsC);
        spawnEggsC.action = () -> tableChecked(spawnEggsItemsE, spawnEggsC.checked);
        Cell<WSection> spawnEggsCell = add(spawnEggsSection).expandX();
        spawnEggsT = spawnEggsSection.add(theme.table()).expandX().widget();

        // Other
        List<Item> otherItemsE = new ArrayList<>();
        WCheckbox otherC = theme.checkbox(hasOther > 0);
        otherSection = theme.section("Other", otherSection != null && otherSection.isExpanded(), otherC);
        otherC.action = () -> tableChecked(otherItemsE, otherC.checked);
        Cell<WSection> otherCell = add(otherSection).expandX();
        otherT = otherSection.add(theme.table()).expandX().widget();

        Consumer<Item> itemForEach = item -> {
            if (item == Items.AIR) return;
            if (setting.filter != null) {
                try {
                    if (!setting.filter.test(item)) return;
                } catch (Exception ignored) {}
            }

            switch (getCategory(item)) {
                case BLOCKS -> {
                    blocksItemsE.add(item);
                    addItem(blocksT, blocksC, item);
                }
                case TOOLS -> {
                    toolsItemsE.add(item);
                    addItem(toolsT, toolsC, item);
                }
                case WEAPONS -> {
                    weaponsItemsE.add(item);
                    addItem(weaponsT, weaponsC, item);
                }
                case ARMOR -> {
                    armorItemsE.add(item);
                    addItem(armorT, armorC, item);
                }
                case FOOD -> {
                    foodItemsE.add(item);
                    addItem(foodT, foodC, item);
                }
                case POTIONS -> {
                    potionsItemsE.add(item);
                    addItem(potionsT, potionsC, item);
                }
                case PROJECTILES -> {
                    projectilesItemsE.add(item);
                    addItem(projectilesT, projectilesC, item);
                }
                case SPAWN_EGGS -> {
                    spawnEggsItemsE.add(item);
                    addItem(spawnEggsT, spawnEggsC, item);
                }
                case OTHER -> {
                    otherItemsE.add(item);
                    addItem(otherT, otherC, item);
                }
            }
        };

        if (filterText.isEmpty()) {
            BuiltInRegistries.ITEM.forEach(itemForEach);
        } else {
            List<Tuple<Item, Integer>> items = new ArrayList<>();
            BuiltInRegistries.ITEM.forEach(item -> {
                int words = Utils.searchInWords(Names.get(item), filterText);
                int diff = Utils.searchLevenshteinDefault(Names.get(item), filterText, false);

                if (words > 0 || diff < Names.get(item).length() / 2) items.add(new Tuple<>(item, -diff));
            });
            items.sort(Comparator.comparingInt(value -> -value.getB()));
            for (Tuple<Item, Integer> pair : items) itemForEach.accept(pair.getA());
        }

        if (blocksT.cells.isEmpty()) list.cells.remove(blocksCell);
        if (toolsT.cells.isEmpty()) list.cells.remove(toolsCell);
        if (weaponsT.cells.isEmpty()) list.cells.remove(weaponsCell);
        if (armorT.cells.isEmpty()) list.cells.remove(armorCell);
        if (foodT.cells.isEmpty()) list.cells.remove(foodCell);
        if (potionsT.cells.isEmpty()) list.cells.remove(potionsCell);
        if (projectilesT.cells.isEmpty()) list.cells.remove(projectilesCell);
        if (spawnEggsT.cells.isEmpty()) list.cells.remove(spawnEggsCell);
        if (otherT.cells.isEmpty()) list.cells.remove(otherCell);

        if (first) {
            int totalCount = (hasBlocks + hasTools + hasWeapons + hasArmor + hasFood + hasPotions + hasProjectiles + hasSpawnEggs + hasOther) / 2;

            if (totalCount <= 20) {
                if (!blocksT.cells.isEmpty()) blocksSection.setExpanded(true);
                if (!toolsT.cells.isEmpty()) toolsSection.setExpanded(true);
                if (!weaponsT.cells.isEmpty()) weaponsSection.setExpanded(true);
                if (!armorT.cells.isEmpty()) armorSection.setExpanded(true);
                if (!foodT.cells.isEmpty()) foodSection.setExpanded(true);
                if (!potionsT.cells.isEmpty()) potionsSection.setExpanded(true);
                if (!projectilesT.cells.isEmpty()) projectilesSection.setExpanded(true);
                if (!spawnEggsT.cells.isEmpty()) spawnEggsSection.setExpanded(true);
                if (!otherT.cells.isEmpty()) otherSection.setExpanded(true);
            } else {
                if (!blocksT.cells.isEmpty()) blocksSection.setExpanded(false);
                if (!toolsT.cells.isEmpty()) toolsSection.setExpanded(false);
                if (!weaponsT.cells.isEmpty()) weaponsSection.setExpanded(false);
                if (!armorT.cells.isEmpty()) armorSection.setExpanded(false);
                if (!foodT.cells.isEmpty()) foodSection.setExpanded(false);
                if (!potionsT.cells.isEmpty()) potionsSection.setExpanded(false);
                if (!projectilesT.cells.isEmpty()) projectilesSection.setExpanded(false);
                if (!spawnEggsT.cells.isEmpty()) spawnEggsSection.setExpanded(false);
                if (!otherT.cells.isEmpty()) otherSection.setExpanded(false);
            }
        }
    }

    private void tableChecked(List<Item> items, boolean checked) {
        boolean changed = false;

        for (Item item : items) {
            if (checked) {
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

    private void addItem(WTable table, WCheckbox tableCheckbox, Item item) {
        table.add(theme.item(DisplayItemUtils.toStack(item)));

        table.add(theme.label(Names.get(item)));

        WCheckbox a = table.add(theme.checkbox(setting.get().contains(item))).expandCellX().right().widget();
        a.action = () -> {
            if (a.checked) {
                setting.get().add(item);
                switch (getCategory(item)) {
                    case BLOCKS -> { if (hasBlocks == 0) tableCheckbox.checked = true; hasBlocks++; }
                    case TOOLS -> { if (hasTools == 0) tableCheckbox.checked = true; hasTools++; }
                    case WEAPONS -> { if (hasWeapons == 0) tableCheckbox.checked = true; hasWeapons++; }
                    case ARMOR -> { if (hasArmor == 0) tableCheckbox.checked = true; hasArmor++; }
                    case FOOD -> { if (hasFood == 0) tableCheckbox.checked = true; hasFood++; }
                    case POTIONS -> { if (hasPotions == 0) tableCheckbox.checked = true; hasPotions++; }
                    case PROJECTILES -> { if (hasProjectiles == 0) tableCheckbox.checked = true; hasProjectiles++; }
                    case SPAWN_EGGS -> { if (hasSpawnEggs == 0) tableCheckbox.checked = true; hasSpawnEggs++; }
                    case OTHER -> { if (hasOther == 0) tableCheckbox.checked = true; hasOther++; }
                }
            } else {
                if (setting.get().remove(item)) {
                    switch (getCategory(item)) {
                        case BLOCKS -> { hasBlocks--; if (hasBlocks == 0) tableCheckbox.checked = false; }
                        case TOOLS -> { hasTools--; if (hasTools == 0) tableCheckbox.checked = false; }
                        case WEAPONS -> { hasWeapons--; if (hasWeapons == 0) tableCheckbox.checked = false; }
                        case ARMOR -> { hasArmor--; if (hasArmor == 0) tableCheckbox.checked = false; }
                        case FOOD -> { hasFood--; if (hasFood == 0) tableCheckbox.checked = false; }
                        case POTIONS -> { hasPotions--; if (hasPotions == 0) tableCheckbox.checked = false; }
                        case PROJECTILES -> { hasProjectiles--; if (hasProjectiles == 0) tableCheckbox.checked = false; }
                        case SPAWN_EGGS -> { hasSpawnEggs--; if (hasSpawnEggs == 0) tableCheckbox.checked = false; }
                        case OTHER -> { hasOther--; if (hasOther == 0) tableCheckbox.checked = false; }
                    }
                }
            }

            setting.onChanged();
        };

        table.row();
    }

    private enum ItemCategory {
        BLOCKS, TOOLS, WEAPONS, ARMOR, FOOD, POTIONS, PROJECTILES, SPAWN_EGGS, OTHER
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
