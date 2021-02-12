/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.screens.WindowScreen;
import minegame159.meteorclient.gui.widgets.WCheckbox;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WSection;
import minegame159.meteorclient.gui.widgets.WTextBox;
import minegame159.meteorclient.settings.EntityTypeListSetting;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.misc.Names;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class EntityTypeListSettingScreen extends WindowScreen {
    private final EntityTypeListSetting setting;
    private final WTextBox filter;

    private String filterText = "";

    private WSection animals, waterAnimals, monsters, ambient, misc;
    int hasAnimal = 0, hasWaterAnimal = 0, hasMonster = 0, hasAmbient = 0, hasMisc = 0;

    public EntityTypeListSettingScreen(EntityTypeListSetting setting) {
        super("Select entities", true);
        this.setting = setting;

        // Filter
        filter = new WTextBox("", 0);
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.getText().trim();

            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        hasAnimal = hasWaterAnimal = hasMonster = hasAmbient = hasMisc = 0;
        for (EntityType<?> entityType : setting.get().keySet()) {
            if (!setting.get().getBoolean(entityType)) continue;

            if (!setting.onlyAttackable || EntityUtils.isAttackable(entityType)) {
                switch (entityType.getSpawnGroup()) {
                    case CREATURE:       hasAnimal++; break;
                    case WATER_CREATURE: hasWaterAnimal++; break;
                    case MONSTER:        hasMonster++; break;
                    case AMBIENT:        hasAmbient++; break;
                    case MISC:           hasMisc++; break;
                }
            }
        }

        add(filter).fillX().expandX();
        row();

        List<EntityType<?>> animalsE = new ArrayList<>();
        boolean expanded = animals != null && animals.isExpanded();
        WCheckbox animalsC = new WCheckbox(hasAnimal > 0);
        animals = new WSection("Animals", false, animalsC);
        if (animals.isExpanded() != expanded) animals.setExpanded(expanded, false);
        animalsC.action = () -> tableChecked(animalsE, animalsC.checked);
        row();

        List<EntityType<?>> waterAnimalsE = new ArrayList<>();
        expanded = waterAnimals != null && waterAnimals.isExpanded();
        WCheckbox waterAnimalsC = new WCheckbox(hasWaterAnimal > 0);
        waterAnimals = new WSection("Water Animals", false, waterAnimalsC);
        if (waterAnimals.isExpanded() != expanded) waterAnimals.setExpanded(expanded, false);
        waterAnimalsC.action = () -> tableChecked(waterAnimalsE, waterAnimalsC.checked);
        row();

        List<EntityType<?>> monstersE = new ArrayList<>();
        expanded = monsters != null && monsters.isExpanded();
        WCheckbox monstersC = new WCheckbox(hasMonster > 0);
        monsters = new WSection("Monsters", false, monstersC);
        if (monsters.isExpanded() != expanded) monsters.setExpanded(expanded, false);
        monstersC.action = () -> tableChecked(monstersE, monstersC.checked);
        row();

        List<EntityType<?>> ambientE = new ArrayList<>();
        expanded = ambient != null && ambient.isExpanded();
        WCheckbox ambientC = new WCheckbox(hasAmbient > 0);
        ambient = new WSection("Ambient", false, ambientC);
        if (ambient.isExpanded() != expanded) ambient.setExpanded(expanded, false);
        ambientC.action = () -> tableChecked(ambientE, ambientC.checked);
        row();

        List<EntityType<?>> miscE = new ArrayList<>();
        expanded = misc != null && misc.isExpanded();
        WCheckbox miscC = new WCheckbox(hasMisc > 0);
        misc = new WSection("Misc", false, miscC);
        if (misc.isExpanded() != expanded) misc.setExpanded(expanded, false);
        miscC.action = () -> tableChecked(miscE, miscC.checked);
        row();

        Consumer<EntityType<?>> entityTypeForEach = entityType -> {
            if (!setting.onlyAttackable || EntityUtils.isAttackable(entityType)) {
                switch (entityType.getSpawnGroup()) {
                    case CREATURE:
                        animalsE.add(entityType);
                        addEntityType(animals, animalsC, entityType);
                        break;
                    case WATER_CREATURE:
                        waterAnimalsE.add(entityType);
                        addEntityType(waterAnimals, waterAnimalsC, entityType);
                        break;
                    case MONSTER:
                        monstersE.add(entityType);
                        addEntityType(monsters, monstersC, entityType);
                        break;
                    case AMBIENT:
                        ambientE.add(entityType);
                        addEntityType(ambient, ambientC, entityType);
                        break;
                    case MISC:
                        miscE.add(entityType);
                        addEntityType(misc, miscC, entityType);
                        break;
                }
            }
        };

        // Sort all entities
        if (filterText.isEmpty()) {
            Registry.ENTITY_TYPE.forEach(entityTypeForEach);
        } else {
            List<Pair<EntityType<?>, Integer>> entities = new ArrayList<>();
            Registry.ENTITY_TYPE.forEach(entity -> {
                int words = Utils.search(Names.get(entity), filterText);
                if (words > 0) entities.add(new Pair<>(entity, words));
            });
            entities.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<EntityType<?>, Integer> pair : entities) entityTypeForEach.accept(pair.getLeft());
        }

        if (animals.getCells().size() > 0) add(animals).fillX().expandX();
        if (waterAnimals.getCells().size() > 0) add(waterAnimals).fillX().expandX();
        if (monsters.getCells().size() > 0) add(monsters).fillX().expandX();
        if (ambient.getCells().size() > 0) add(ambient).fillX().expandX();
        if (misc.getCells().size() > 0) add(misc).fillX().expandX();

        int totalCount = (hasWaterAnimal + waterAnimals.getCells().size() + monsters.getCells().size() + ambient.getCells().size() + misc.getCells().size()) / 2;

        if (totalCount <= GuiConfig.get().countListSettingScreen) {
            if (GuiConfig.get().expandListSettingScreen) {
                if (animals.getCells().size() > 0) animals.setExpanded(true, false);
                if (waterAnimals.getCells().size() > 0) waterAnimals.setExpanded(true, false);
                if (monsters.getCells().size() > 0) monsters.setExpanded(true, false);
                if (ambient.getCells().size() > 0) ambient.setExpanded(true, false);
                if (misc.getCells().size() > 0) misc.setExpanded(true, false);
            }
        } else {
            if (GuiConfig.get().collapseListSettingScreen) {
                if (animals.getCells().size() > 0) animals.setExpanded(false, false);
                if (waterAnimals.getCells().size() > 0) waterAnimals.setExpanded(false, false);
                if (monsters.getCells().size() > 0) monsters.setExpanded(false, false);
                if (ambient.getCells().size() > 0) ambient.setExpanded(false, false);
                if (misc.getCells().size() > 0) misc.setExpanded(false, false);
            }
        }
    }

    private void tableChecked(List<EntityType<?>> entityTypes, boolean checked) {
        boolean changed = false;

        for (EntityType<?> entityType : entityTypes) {
            if (checked) {
                setting.get().put(entityType, true);
                changed = true;
            } else {
                if (setting.get().removeBoolean(entityType)) {
                    changed = true;
                }
            }
        }

        if (changed) {
            clear();
            initWidgets();
            setting.changed();
        }
    }

    private void addEntityType(WSection table, WCheckbox tableCheckbox, EntityType<?> entityType) {
        table.add(new WLabel(Names.get(entityType)));
        WCheckbox a = table.add(new WCheckbox(setting.get().getBoolean(entityType))).fillX().right().getWidget();
        a.action = () -> {
            if (a.checked) {
                setting.get().put(entityType, true);
                switch (entityType.getSpawnGroup()) {
                    case CREATURE:       if (hasAnimal == 0) tableCheckbox.checked = true; hasAnimal++; break;
                    case WATER_CREATURE: if (hasWaterAnimal == 0) tableCheckbox.checked = true; hasWaterAnimal++; break;
                    case MONSTER:        if (hasMonster == 0) tableCheckbox.checked = true; hasMonster++; break;
                    case AMBIENT:        if (hasAmbient == 0) tableCheckbox.checked = true; hasAmbient++; break;
                    case MISC:           if (hasMisc == 0) tableCheckbox.checked = true; hasMisc++; break;
                }
            } else {
                if (setting.get().removeBoolean(entityType)) {
                    switch (entityType.getSpawnGroup()) {
                        case CREATURE:       hasAnimal--; if (hasAnimal == 0) tableCheckbox.checked = false; break;
                        case WATER_CREATURE: hasWaterAnimal--; if (hasWaterAnimal == 0) tableCheckbox.checked = false; break;
                        case MONSTER:        hasMonster--; if (hasMonster == 0) tableCheckbox.checked = false; break;
                        case AMBIENT:        hasAmbient--; if (hasAmbient == 0) tableCheckbox.checked = false; break;
                        case MISC:           hasMisc--; if (hasMisc == 0)  tableCheckbox.checked = false; break;
                    }
                }
            }

            setting.changed();
        };

        table.row();
    }
}
