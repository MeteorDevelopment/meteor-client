/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.base;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.GroupedSetSetting;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.utils.render.color.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class GroupedSetSettingScreen<T, S extends GroupedSetSetting<T>> extends WindowScreen {
    protected final S setting;
    private final Iterable<T> registry;
    private final GroupedSetSetting.Groups<T> groups;

    private WTable table;
    private String filterText = "";

    private GroupedSetSetting.Groups<T>.Group expanded;

    public GroupedSetSettingScreen(GuiTheme theme, String title, S setting, GroupedSetSetting.Groups<T> groups, Iterable<T> registry) {
        super(theme, title);

        this.registry = registry;
        this.setting = setting;
        this.groups = groups;
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

        List<ItemUnion> list = new ArrayList<>(groups.getAll().stream().map(ItemUnion::new).toList());

        registry.forEach((t) -> list.add(new ItemUnion(t)));

        // Left (all)
        WTable left = abc(list, true, t -> {
            addValue(t);

            if (t.t != null) {
                T v = getAdditionalValue(t.t);
                if (v != null) addValue(t);
            }
        });

        if (!left.cells.isEmpty()) table.add(theme.verticalSeparator()).expandWidgetY();

        list.clear();
        list.addAll(setting.get().getGroups().stream().map(ItemUnion::new).toList());

        setting.get().getImmediate().forEach((t) -> list.add(new ItemUnion(t)));

        // Right (selected)
        WTable right = abc(list, false, t -> {
            removeValue(t);

            if (t.t != null) {
                T v = getAdditionalValue(t.t);
                if (v != null) removeValue(t);
            }
        });

        postWidgets(left, right);
    }

    private WWidget groupLabel(GroupedSetSetting.Groups<T>.Group s) {
        Color color = entireGroupExcluded(s) ? new Color(200, 100, 10) : Color.ORANGE;
        if (s.showIcon.get()) return theme.itemWithLabel(s.icon.get().asItem().getDefaultStack(), "@"+s.name.get()).color(color);
        else return theme.label(" @"+s.name.get()).color(color);
    }

    private WTable abc(Iterable<ItemUnion> iterable, boolean isLeft, Consumer<ItemUnion> buttonAction) {
        // Create
        Cell<WTable> cell = this.table.add(theme.table()).top();
        WTable table = cell.widget();


        // Sort
        Predicate<ItemUnion> predicate = isLeft
            ? v -> Boolean.TRUE.equals(v.map(
                t -> this.includeValue(t) && !setting.get().getImmediate().contains(t),
            s -> !entireGroupExcluded(s) && !setting.get().getGroups().contains(s)))
            : v -> true;

        Iterable<ItemUnion> sorted = SortingHelper.sort(iterable, predicate, v -> v.map(this::getValueNames, s -> new String[]{"@"+s.name.get()}), filterText);

        sorted.forEach(v -> {
            table.add(v.map(this::getValueWidget, s -> {

                WVerticalList vlist = theme.verticalList();
                WTable hlist = vlist.add(theme.table()).widget();

                boolean e = expanded == s;

                WButton expand = hlist.add(theme.button(e ? GuiRenderer.TRIANGLE : GuiRenderer.CIRCLE)).widget();
                expand.action = () -> {
                    expanded = e ? null : s;
                    reload();
                };

                hlist.add(groupLabel(s));

                WTable subtable = vlist.add(theme.table()).widget();

                if (e) {
                    for (GroupedSetSetting.Groups<T>.Group inc : s.getGroups()) {
                        subtable.add(theme.label("   -> "));
                        subtable.add(groupLabel(inc));
                        subtable.row();
                    }

                    Iterable<T> subitems = SortingHelper.sortWithPriority(s.getImmediate(), (t)->true, this::getValueNames, "", (T a, T b) -> includeValue(a) == includeValue(b) ? 0 : includeValue(a) ? -1 : 1);

                    subitems.forEach(t -> {
                        subtable.add(theme.label("   -> "));
                        subtable.add(getValueWidget(t));
                        subtable.row();
                    });
                }

                return vlist;
            }));

            if (v.s != null) {
                WButton edit = table.add(theme.button(GuiRenderer.EDIT)).right().top().widget();
                edit.action = () -> MeteorClient.mc.setScreen(new EditListGroupScreen(theme, v.s, () -> {
                    invalidateTable();
                    reload();
                }));
            }

            WPressable button = table.add(isLeft ? theme.plus() : theme.minus()).expandCellX().right().top().widget();
            button.action = () -> buttonAction.accept(v);

            table.row();
        });

        if (!table.cells.isEmpty()) cell.expandX();

        return table;
    }

    private boolean entireGroupExcluded(GroupedSetSetting.Groups<T>.Group s) {
        return !s.anyMatch(this::includeValue);
    }

    protected void invalidateTable() {
        table.clear();
        initTable();
    }

    protected void addValue(ItemUnion value) {
        if (value.t != null) {
            setting.get().add(value.t);
            setting.onChanged();
            invalidateTable();
        } else if (value.s != null) {
            setting.get().add(value.s);
            setting.onChanged();
            invalidateTable();
        }
    }

    protected void removeValue(ItemUnion value) {
       if (value.t != null) {
            setting.get().remove(value.t);
            setting.onChanged();
            invalidateTable();
        } else if (value.s != null) {
            setting.get().remove(value.s);
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

    protected class ItemUnion {
        private final T t;
        private final GroupedSetSetting.Groups<T>.Group s;

        public ItemUnion(T t) {
            this.s = null;
            this.t = t;
        }

        public ItemUnion(GroupedSetSetting.Groups<T>.Group s) {
            this.s = s;
            this.t = null;
        }

        public <R> R map(Function<T, R> a, Function<GroupedSetSetting.Groups<T>.Group, R> b) {
            if (t != null) return a.apply(t);
            else if (s != null) return b.apply(s);
            return null;
        }
    }

    public class EditListGroupScreen extends EditSystemScreen<GroupedSetSetting.Groups<T>.Group> {
        public EditListGroupScreen(GuiTheme theme, GroupedSetSetting.Groups<T>.Group value, Runnable reload) {
            super(theme, value, reload);
        }

        @Override
        public GroupedSetSetting.Groups<T>.Group create() {
            return null;
        }

        @Override
        public boolean save() {
            value.builtin = false;
            return true;
        }

        @Override
        public Settings getSettings() {
            return value.settings;
        }
    }
}
