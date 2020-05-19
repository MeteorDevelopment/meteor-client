package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class EntityTypeListSettingScreen extends WindowScreen {
    private final Setting<List<EntityType<?>>> setting;
    private final WTextBox filter;

    private WCollapsableTable animals, waterAnimals, monsters, ambient, misc;
    int hasAnimal = 0, hasWaterAnimal = 0, hasMonster = 0, hasAmbient = 0, hasMisc = 0;

    public EntityTypeListSettingScreen(Setting<List<EntityType<?>>> setting) {
        super("Select entities", true);
        this.setting = setting;

        // Filter
        filter = new WTextBox("", 0);
        filter.setFocused(true);
        filter.action = textBox -> {
            clear();
            initWidgets();
        };

        initWidgets();
    }

    private void initWidgets() {
        hasAnimal = hasWaterAnimal = hasMonster = hasAmbient = hasMisc = 0;
        for (EntityType<?> entityType : setting.get()) {
            switch (entityType.getCategory()) {
                case CREATURE:       hasAnimal++; break;
                case WATER_CREATURE: hasWaterAnimal++; break;
                case MONSTER:        hasMonster++; break;
                case AMBIENT:        hasAmbient++; break;
                case MISC:           hasMisc++; break;
            }
        }

        add(filter).fillX().expandX();
        row();

        List<EntityType<?>> animalsE = new ArrayList<>();
        boolean expanded = animals != null && animals.expanded;
        animals = new WCollapsableTable("Animals");
        if (animals.expanded != expanded) animals.setExpanded(expanded);
        animals.enabledScroll = false;
        WCheckbox animalsC = animals.header.add(new WCheckbox(hasAnimal > 0)).getWidget();
        animalsC.action = checkbox -> tableChecked(animalsE, checkbox.checked);
        row();

        List<EntityType<?>> waterAnimalsE = new ArrayList<>();
        expanded = waterAnimals != null && waterAnimals.expanded;
        waterAnimals = new WCollapsableTable("Water Animals");
        if (waterAnimals.expanded != expanded) waterAnimals.setExpanded(expanded);
        waterAnimals.enabledScroll = false;
        WCheckbox waterAnimalsC = waterAnimals.header.add(new WCheckbox(hasWaterAnimal > 0)).getWidget();
        waterAnimalsC.action = checkbox -> tableChecked(waterAnimalsE, checkbox.checked);
        row();

        List<EntityType<?>> monstersE = new ArrayList<>();
        expanded = monsters != null && monsters.expanded;
        monsters = new WCollapsableTable("Monsters");
        if (monsters.expanded != expanded) monsters.setExpanded(expanded);
        monsters.enabledScroll = false;
        WCheckbox monstersC = monsters.header.add(new WCheckbox(hasMonster > 0)).getWidget();
        monstersC.action = checkbox -> tableChecked(monstersE, checkbox.checked);
        row();

        List<EntityType<?>> ambientE = new ArrayList<>();
        expanded = ambient != null && ambient.expanded;
        ambient = new WCollapsableTable("Ambient");
        if (ambient.expanded != expanded) ambient.setExpanded(expanded);
        ambient.enabledScroll = false;
        WCheckbox ambientC = ambient.header.add(new WCheckbox(hasAmbient > 0)).getWidget();
        ambientC.action = checkbox -> tableChecked(ambientE, checkbox.checked);
        row();

        List<EntityType<?>> miscE = new ArrayList<>();
        expanded = misc != null && misc.expanded;
        misc = new WCollapsableTable("Misc");
        if (misc.expanded != expanded) misc.setExpanded(expanded);
        misc.enabledScroll = false;
        WCheckbox miscC = misc.header.add(new WCheckbox(hasMisc > 0)).getWidget();
        miscC.action = checkbox -> tableChecked(miscE, checkbox.checked);
        row();

        Consumer<EntityType<?>> entityTypeForEach = entityType -> {
            switch (entityType.getCategory()) {
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
        };

        // Sort all entities
        if (filter.text.isEmpty()) {
            Registry.ENTITY_TYPE.forEach(entityTypeForEach);
        } else {
            List<Pair<EntityType<?>, Integer>> entities = new ArrayList<>();
            Registry.ENTITY_TYPE.forEach(entity -> {
                int words = Utils.search(entity.getName().asFormattedString(), filter.text);
                if (words > 0) entities.add(new Pair<>(entity, words));
            });
            entities.sort(Comparator.comparingInt(value -> -value.getRight()));
            for (Pair<EntityType<?>, Integer> pair : entities) entityTypeForEach.accept(pair.getLeft());
        }

        if (animals.table.getCells().size() > 0) add(animals).fillX().expandX();
        if (waterAnimals.table.getCells().size() > 0) add(waterAnimals).fillX().expandX();
        if (monsters.table.getCells().size() > 0) add(monsters).fillX().expandX();
        if (ambient.table.getCells().size() > 0) add(ambient).fillX().expandX();
        if (misc.table.getCells().size() > 0) add(misc).fillX().expandX();
    }

    private void tableChecked(List<EntityType<?>> entityTypes, boolean checked) {
        boolean changed = false;

        for (EntityType<?> entityType : entityTypes) {
            if (checked) {
                if (!setting.get().contains(entityType)) {
                    setting.get().add(entityType);
                    changed = true;
                }
            } else {
                if (setting.get().contains(entityType)) {
                    setting.get().remove(entityType);
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

    private void addEntityType(WTable table, WCheckbox tableCheckbox, EntityType<?> entityType) {
        table.add(new WLabel(entityType.getName().asString()));
        table.add(new WCheckbox(setting.get().contains(entityType))).fillX().right().getWidget().action = checkbox -> {
            if (checkbox.checked) {
                if (!setting.get().contains(entityType)) {
                    setting.get().add(entityType);
                    switch (entityType.getCategory()) {
                        case CREATURE:       if (hasAnimal == 0) tableCheckbox.checked = true; hasAnimal++; break;
                        case WATER_CREATURE: if (hasWaterAnimal == 0) tableCheckbox.checked = true; hasWaterAnimal++; break;
                        case MONSTER:        if (hasMonster == 0) tableCheckbox.checked = true; hasMonster++; break;
                        case AMBIENT:        if (hasAmbient == 0) tableCheckbox.checked = true; hasAmbient++; break;
                        case MISC:           if (hasMisc == 0) tableCheckbox.checked = true; hasMisc++; break;
                    }
                }
            } else {
                if (setting.get().remove(entityType)) {
                    switch (entityType.getCategory()) {
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
