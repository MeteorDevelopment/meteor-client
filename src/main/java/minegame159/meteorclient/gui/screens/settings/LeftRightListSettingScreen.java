/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.WindowScreen;
import minegame159.meteorclient.gui.utils.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.WTable;
import minegame159.meteorclient.gui.widgets.input.WTextBox;
import minegame159.meteorclient.gui.widgets.pressable.WPressable;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public abstract class LeftRightListSettingScreen<T> extends WindowScreen {
    protected final Setting<List<T>> setting;
    private final WTextBox filter;

    private String filterText = "";

    private WTable table;

    public LeftRightListSettingScreen(GuiTheme theme, String title, Setting<List<T>> setting, Registry<T> registry) {
        super(theme, title);

        this.setting = setting;

        // Filter
        filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            initWidgets(registry);
        };

        table = add(theme.table()).expandX().widget();

        initWidgets(registry);
    }

    private void initWidgets(Registry<T> registry) {
        // Left (all)
        WTable left = abc(pairs -> registry.forEach(t -> {
            if (skipValue(t) || setting.get().contains(t)) return;

            int words = Utils.search(getValueName(t), filterText);
            if (words > 0) pairs.add(new Pair<>(t, words));
        }), true, t -> {
            addValue(registry, t);

            T v = getAdditionalValue(t);
            if (v != null) addValue(registry, v);
        });

        if (left.cells.size() > 0) table.add(theme.verticalSeparator()).expandWidgetY();

        // Right (selected)
        abc(pairs -> {
            for (T value : setting.get()) {
                if (skipValue(value)) continue;

                int words = Utils.search(getValueName(value), filterText);
                if (words > 0) pairs.add(new Pair<>(value, words));
            }
        }, false, t -> {
            removeValue(registry, t);

            T v = getAdditionalValue(t);
            if (v != null) removeValue(registry, v);
        });
    }

    private void addValue(Registry<T> registry, T value) {
        if (!setting.get().contains(value)) {
            setting.get().add(value);

            setting.changed();
            table.clear();
            initWidgets(registry);
        }
    }

    private void removeValue(Registry<T> registry, T value) {
        if (setting.get().remove(value)) {
            setting.changed();
            table.clear();
            initWidgets(registry);
        }
    }

    private WTable abc(Consumer<List<Pair<T, Integer>>> addValues, boolean isLeft, Consumer<T> buttonAction) {
        // Create
        Cell<WTable> cell = this.table.add(theme.table()).top();
        WTable table = cell.widget();

        Consumer<T> forEach = t -> {
            if (!includeValue(t)) return;

            table.add(getValueWidget(t));

            WPressable button = table.add(isLeft ? theme.plus() : theme.minus()).expandCellX().right().widget();
            button.action = () -> buttonAction.accept(t);

            table.row();
        };

        // Sort
        List<Pair<T, Integer>> values = new ArrayList<>();
        addValues.accept(values);
        if (!filterText.isEmpty()) values.sort(Comparator.comparingInt(value -> -value.getRight()));
        for (Pair<T, Integer> pair : values) forEach.accept(pair.getLeft());

        if (table.cells.size() > 0) cell.expandX();

        return table;
    }

    protected boolean includeValue(T value) {
        return true;
    }

    protected abstract WWidget getValueWidget(T value);

    protected abstract String getValueName(T value);

    protected boolean skipValue(T value) {
        return false;
    }

    protected T getAdditionalValue(T value) {
        return null;
    }
}
