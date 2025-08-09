/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.base;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;

public abstract class CollectionMapSettingScreen<K, V> extends WindowScreen {
    private final Setting<?> setting;
    protected final Map<K, V> map;
    private final Iterable<K> registry;

    private WTable table;
    private String filterText = "";

    public CollectionMapSettingScreen(GuiTheme theme, String title, Setting<?> setting, Map<K, V> map, Iterable<K> registry) {
        super(theme, title);

        this.setting = setting;
        this.map = map;
        this.registry = registry;
    }

    @Override
    public void initWidgets() {
        // Filter
        WTextBox filter = add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.action = () -> {
            filterText = filter.get().trim();

            table.clear();
            initTable();
        };

        table = add(theme.table()).expandX().widget();

        initTable();
    }

    private void initTable() {
        Comparator<K> prioritizeChanged = Comparator.comparing(key -> !(map.get(key) instanceof IChangeable changeable && changeable.isChanged()));
        Iterable<K> sorted = SortingHelper.sortWithPriority(registry, this::includeValue, this::getValueNames, filterText, prioritizeChanged);

        sorted.forEach(t -> {
            @Nullable V data = map.get(t);
            boolean isChanged = data instanceof IChangeable changeable && changeable.isChanged();

            table.add(getValueWidget(t)).expandCellX();
            table.add(theme.label(isChanged ? "*" : " "));
            table.add(getDataWidget(t, data));

            WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
            reset.action = () -> removeValue(t);
            reset.tooltip = "Reset";

            table.row();
        });
    }

    protected void invalidateTable() {
        table.clear();
        initTable();
    }

    protected void removeValue(K value) {
        if (map.remove(value) != null) {
            setting.onChanged();
            invalidateTable();
        }
    }

    protected boolean includeValue(K value) {
        return true;
    }

    protected abstract WWidget getValueWidget(K value);

    protected abstract WWidget getDataWidget(K value, @Nullable V data);

    protected abstract String[] getValueNames(K value);
}
