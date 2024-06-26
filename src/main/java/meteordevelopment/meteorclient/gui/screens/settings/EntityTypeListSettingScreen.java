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
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class EntityTypeListSettingScreen extends WindowScreen {
    private final EntityTypeListSetting setting;

    private WVerticalList list;
    private final WTextBox filter;

    private String filterText = "";

    private WSection animals, waterAnimals, monsters, ambient, misc;
    private WTable animalsT, waterAnimalsT, monstersT, ambientT, miscT;
    int hasAnimal = 0, hasWaterAnimal = 0, hasMonster = 0, hasAmbient = 0, hasMisc = 0;

    public EntityTypeListSettingScreen(GuiTheme theme, EntityTypeListSetting setting) {
        super(theme, "Select entities");
        this.setting = setting;

        // Filter
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
        hasAnimal = hasWaterAnimal = hasMonster = hasAmbient = hasMisc = 0;

        for (EntityType<?> entityType : setting.get()) {
            if (setting.filter == null || setting.filter.test(entityType)) {
                switch (entityType.getSpawnGroup()) {
                    case CREATURE -> hasAnimal++;
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> hasWaterAnimal++;
                    case MONSTER -> hasMonster++;
                    case AMBIENT -> hasAmbient++;
                    case MISC -> hasMisc++;
                }
            }
        }

        boolean first = animals == null;

        // Animals
        List<EntityType<?>> animalsE = new ArrayList<>();
        WCheckbox animalsC = theme.checkbox(hasAnimal > 0);

        animals = theme.section("Animals", animals != null && animals.isExpanded(), animalsC);
        animalsC.action = () -> tableChecked(animalsE, animalsC.checked);

        Cell<WSection> animalsCell = add(animals).expandX();
        animalsT = animals.add(theme.table()).expandX().widget();

        // Water animals
        List<EntityType<?>> waterAnimalsE = new ArrayList<>();
        WCheckbox waterAnimalsC = theme.checkbox(hasWaterAnimal > 0);

        waterAnimals = theme.section("Water Animals", waterAnimals != null && waterAnimals.isExpanded(), waterAnimalsC);
        waterAnimalsC.action = () -> tableChecked(waterAnimalsE, waterAnimalsC.checked);

        Cell<WSection> waterAnimalsCell = add(waterAnimals).expandX();
        waterAnimalsT = waterAnimals.add(theme.table()).expandX().widget();

        // Monsters
        List<EntityType<?>> monstersE = new ArrayList<>();
        WCheckbox monstersC = theme.checkbox(hasMonster > 0);

        monsters = theme.section("Monsters", monsters != null && monsters.isExpanded(), monstersC);
        monstersC.action = () -> tableChecked(monstersE, monstersC.checked);

        Cell<WSection> monstersCell = add(monsters).expandX();
        monstersT = monsters.add(theme.table()).expandX().widget();

        // Ambient
        List<EntityType<?>> ambientE = new ArrayList<>();
        WCheckbox ambientC = theme.checkbox(hasAmbient > 0);

        ambient = theme.section("Ambient", ambient != null && ambient.isExpanded(), ambientC);
        ambientC.action = () -> tableChecked(ambientE, ambientC.checked);

        Cell<WSection> ambientCell = add(ambient).expandX();
        ambientT = ambient.add(theme.table()).expandX().widget();

        // Misc
        List<EntityType<?>> miscE = new ArrayList<>();
        WCheckbox miscC = theme.checkbox(hasMisc > 0);

        misc = theme.section("Misc", misc != null && misc.isExpanded(), miscC);
        miscC.action = () -> tableChecked(miscE, miscC.checked);

        Cell<WSection> miscCell = add(misc).expandX();
        miscT = misc.add(theme.table()).expandX().widget();

        Consumer<EntityType<?>> entityTypeForEach = entityType -> {
            if (setting.filter == null || setting.filter.test(entityType)) {
                switch (entityType.getSpawnGroup()) {
                    case CREATURE -> {
                        animalsE.add(entityType);
                        addEntityType(animalsT, animalsC, entityType);
                    }
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> {
                        waterAnimalsE.add(entityType);
                        addEntityType(waterAnimalsT, waterAnimalsC, entityType);
                    }
                    case MONSTER -> {
                        monstersE.add(entityType);
                        addEntityType(monstersT, monstersC, entityType);
                    }
                    case AMBIENT -> {
                        ambientE.add(entityType);
                        addEntityType(ambientT, ambientC, entityType);
                    }
                    case MISC -> {
                        miscE.add(entityType);
                        addEntityType(miscT, miscC, entityType);
                    }
                }
            }
        };

        // Sort all entities
        if (filterText.isEmpty()) {
            Registries.ENTITY_TYPE.forEach(entityTypeForEach);
        } else {
            List<Pair<EntityType<?>, Integer>> entities = new ArrayList<>();
            Registries.ENTITY_TYPE.forEach(entity -> {
                int words = Utils.searchInWords(Names.get(entity), filterText);
                int diff = Utils.searchLevenshteinDefault(Names.get(entity), filterText, false);

                if (words > 0 || diff < Names.get(entity).length() / 2) entities.add(new Pair<>(entity, -diff));
            });
            entities.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<EntityType<?>, Integer> pair : entities) entityTypeForEach.accept(pair.getLeft());
        }

        if (animalsT.cells.isEmpty()) list.cells.remove(animalsCell);
        if (waterAnimalsT.cells.isEmpty()) list.cells.remove(waterAnimalsCell);
        if (monstersT.cells.isEmpty()) list.cells.remove(monstersCell);
        if (ambientT.cells.isEmpty()) list.cells.remove(ambientCell);
        if (miscT.cells.isEmpty()) list.cells.remove(miscCell);

        if (first) {
            int totalCount = (hasWaterAnimal + waterAnimals.cells.size() + monsters.cells.size() + ambient.cells.size() + misc.cells.size()) / 2;

            if (totalCount <= 20) {
                if (!animalsT.cells.isEmpty()) animals.setExpanded(true);
                if (!waterAnimalsT.cells.isEmpty()) waterAnimals.setExpanded(true);
                if (!monstersT.cells.isEmpty()) monsters.setExpanded(true);
                if (!ambientT.cells.isEmpty()) ambient.setExpanded(true);
                if (!miscT.cells.isEmpty()) misc.setExpanded(true);
            }
            else {
                if (!animalsT.cells.isEmpty()) animals.setExpanded(false);
                if (!waterAnimalsT.cells.isEmpty()) waterAnimals.setExpanded(false);
                if (!monstersT.cells.isEmpty()) monsters.setExpanded(false);
                if (!ambientT.cells.isEmpty()) ambient.setExpanded(false);
                if (!miscT.cells.isEmpty()) misc.setExpanded(false);
            }
        }
    }

    private void tableChecked(List<EntityType<?>> entityTypes, boolean checked) {
        boolean changed = false;

        for (EntityType<?> entityType : entityTypes) {
            if (checked) {
                setting.get().add(entityType);
                changed = true;
            } else {
                if (setting.get().remove(entityType)) {
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

    private void addEntityType(WTable table, WCheckbox tableCheckbox, EntityType<?> entityType) {
        table.add(theme.label(Names.get(entityType)));

        WCheckbox a = table.add(theme.checkbox(setting.get().contains(entityType))).expandCellX().right().widget();
        a.action = () -> {
            if (a.checked) {
                setting.get().add(entityType);
                switch (entityType.getSpawnGroup()) {
                    case CREATURE -> {
                        if (hasAnimal == 0) tableCheckbox.checked = true;
                        hasAnimal++;
                    }
                    case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> {
                        if (hasWaterAnimal == 0) tableCheckbox.checked = true;
                        hasWaterAnimal++;
                    }
                    case MONSTER -> {
                        if (hasMonster == 0) tableCheckbox.checked = true;
                        hasMonster++;
                    }
                    case AMBIENT -> {
                        if (hasAmbient == 0) tableCheckbox.checked = true;
                        hasAmbient++;
                    }
                    case MISC -> {
                        if (hasMisc == 0) tableCheckbox.checked = true;
                        hasMisc++;
                    }
                }
            } else {
                if (setting.get().remove(entityType)) {
                    switch (entityType.getSpawnGroup()) {
                        case CREATURE -> {
                            hasAnimal--;
                            if (hasAnimal == 0) tableCheckbox.checked = false;
                        }
                        case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> {
                            hasWaterAnimal--;
                            if (hasWaterAnimal == 0) tableCheckbox.checked = false;
                        }
                        case MONSTER -> {
                            hasMonster--;
                            if (hasMonster == 0) tableCheckbox.checked = false;
                        }
                        case AMBIENT -> {
                            hasAmbient--;
                            if (hasAmbient == 0) tableCheckbox.checked = false;
                        }
                        case MISC -> {
                            hasMisc--;
                            if (hasMisc == 0) tableCheckbox.checked = false;
                        }
                    }
                }
            }

            setting.onChanged();
        };

        table.row();
    }
}
