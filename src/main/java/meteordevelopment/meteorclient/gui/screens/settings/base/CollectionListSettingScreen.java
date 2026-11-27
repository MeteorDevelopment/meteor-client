/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.base;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.config.Config;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class CollectionListSettingScreen<T> extends WindowScreen {
    private final boolean syncWidths = Config.get().syncListSettingWidths.get();
    protected final Setting<?> setting;
    protected final Collection<T> collection;
    private final Iterable<T> registry;

    private Iterable<T> leftContent;
    private Iterable<T> rightContent;

    private WTable table;
    private WTextBox filter;
    private String filterText = "";

    public CollectionListSettingScreen(GuiTheme theme, String title, Setting<?> setting, Collection<T> collection, Iterable<T> registry) {
        super(theme, title);

        this.registry = registry;
        this.setting = setting;
        this.collection = collection;
    }

    @Override
    public void initWidgets() {
        // Initialize table contents
        WTable left = initList(registry, true, t -> {
            addValue(t);

            T v = getAdditionalValue(t);
            if (v != null) addValue(v);
        });

        WTable right = initList(collection, false, t -> {
            removeValue(t);

            T v = getAdditionalValue(t);
            if (v != null) removeValue(v);
        });

        // Create header
        WHorizontalList header = add(theme.horizontalList()).expandX().widget();

        // Add All
        if (!left.cells.isEmpty()) {
            WButton addAll = header.add(theme.button("»")).expandWidgetX().widget();
            addAll.action = () -> {
                leftContent.forEach(collection::add);
                invalidateTable();
            };
        }

        // Filter
        if (filter == null) {
            filter = header.add(theme.textBox(filterText)).minWidth(400).expandX().widget();
            filter.setFocused(true);
            filter.action = () -> {
                filterText = filter.get().trim();
                invalidateTable();
            };
        } else {
            // keep cursor position
            header.add(filter).minWidth(400).expandX();
        }

        // Remove All
        if (!right.cells.isEmpty()) {
            WButton removeAll = header.add(theme.button("«")).expandWidgetX().widget();
            removeAll.action = () -> {
                if (filterText.isEmpty()) collection.clear();
                else rightContent.forEach(collection::remove); // todo potential O(n^2) on List setting types
                invalidateTable();
            };
        }

        table = add(theme.table()).expandX().widget();

        // Format & add lists
        table = add(theme.table()).expandX().widget();

        if (syncWidths || !left.cells.isEmpty()) {
            addList(left);
        }

        if (syncWidths || (!left.cells.isEmpty() && !right.cells.isEmpty())) {
            table.add(theme.verticalSeparator()).expandWidgetY();
        }

        if (syncWidths || !right.cells.isEmpty()) {
            addList(right);
        }

        postWidgets(left, right);
    }

    private WTable initList(Iterable<T> iterable, boolean isLeft, Consumer<T> buttonAction) {
        WTable table = theme.table();

        // Sort
        Predicate<T> predicate = isLeft
            ? value -> this.includeValue(value) && !collection.contains(value)
            : this::includeValue;

        Iterable<T> sorted = SortingHelper.sort(iterable, predicate, this::getValueNames, filterText);

        if (isLeft) leftContent = sorted;
        else rightContent = sorted;

        sorted.forEach(t -> {
            table.add(getValueWidget(t));

            WPressable button = table.add(isLeft ? theme.plus() : theme.minus()).expandCellX().right().widget();
            button.action = () -> buttonAction.accept(t);

            table.row();
        });

        return table;
    }

    private void addList(WTable listTable) {
        Cell<WTable> cell = this.table.add(listTable).top();
        if (syncWidths) cell.group("sync-width");
        if (!table.cells.isEmpty()) cell.expandX();
    }

    protected void invalidateTable() {
        this.clear();
        initWidgets();
    }

    protected void addValue(T value) {
        if (!collection.contains(value)) {
            collection.add(value);
            setting.onChanged();
            invalidateTable();
        }
    }

    protected void removeValue(T value) {
        if (collection.remove(value)) {
            setting.onChanged();
            invalidateTable();
        }
    }

    protected void postWidgets(WTable left, WTable right) {}

    protected boolean includeValue(T value) {
        return true;
    }

    protected abstract WWidget getValueWidget(T value);

    protected abstract String[] getValueNames(T value);

    protected T getAdditionalValue(T value) {
        return null;
    }
}
