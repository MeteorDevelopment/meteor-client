/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.base;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public abstract class CollectionListSettingScreen<T> extends WindowScreen {
    protected final Setting<?> setting;
    protected final Collection<T> collection;
    private final Iterable<T> registry;

    private WTextBox filter;
    private String filterText = "";

    private WTable table;

    public CollectionListSettingScreen(GuiTheme theme, String title, Setting<?> setting, Collection<T> collection, Iterable<T> registry) {
        super(theme, title);

        this.registry = registry;
        this.setting = setting;
        this.collection = collection;
    }

    @Override
    public void initWidgets() {
        // Filter
        filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            initTables();
        };

        table = add(theme.table()).expandX().widget();

        initTables();
    }

    private void initTables() {
        // Left (all)
        WTable left = abc(pairs -> registry.forEach(t -> {
            if ((!includeValue(t)) || collection.contains(t)) return;

            int words = Utils.searchInWords(getValueName(t), filterText);
            int diff = Utils.searchLevenshteinDefault(getValueName(t), filterText, false);
            if (words > 0 || diff <= getValueName(t).length() / 2) pairs.add(new Pair<>(t, -diff));
        }), true, t -> {
            addValue(t);

            T v = getAdditionalValue(t);
            if (v != null) addValue(v);
        });

        if (!left.cells.isEmpty()) table.add(theme.verticalSeparator()).expandWidgetY();

        // Right (selected)
        WTable right = abc(pairs -> {
            for (T value : collection) {
                if (!includeValue(value)) continue;

                int words = Utils.searchInWords(getValueName(value), filterText);
                int diff = Utils.searchLevenshteinDefault(getValueName(value), filterText, false);
                if (words > 0 || diff <= getValueName(value).length() / 2) pairs.add(new Pair<>(value, -diff));
            }
        }, false, t -> {
            removeValue(t);

            T v = getAdditionalValue(t);
            if (v != null) removeValue(v);
        });

        postWidgets(left, right);
    }

    private WTable abc(Consumer<List<Pair<T, Integer>>> addValues, boolean isLeft, Consumer<T> buttonAction) {
        // Create
        Cell<WTable> cell = this.table.add(theme.table()).top();
        WTable table = cell.widget();

        Consumer<T> forEach = t -> {
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

        if (!table.cells.isEmpty()) cell.expandX();

        return table;
    }

    protected void addValue(T value) {
        if (!collection.contains(value)) {
            collection.add(value);

            setting.onChanged();
            table.clear();
            initWidgets();
        }
    }

    protected void removeValue(T value) {
        if (collection.remove(value)) {
            setting.onChanged();
            table.clear();
            initWidgets();
        }
    }

    protected void postWidgets(WTable left, WTable right) {}

    protected boolean includeValue(T value) {
        return true;
    }

    protected abstract WWidget getValueWidget(T value);

    protected abstract String getValueName(T value);

    protected T getAdditionalValue(T value) {
        return null;
    }
}
