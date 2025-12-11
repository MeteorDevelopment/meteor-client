/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings.base;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.GroupedSetSetting;
import meteordevelopment.meteorclient.settings.groups.IGroup;
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

    public GroupedSetSettingScreen(GuiTheme theme, String title, S setting, GroupedSetSetting.Groups<T> groups, Iterable<T> registry) {
        super(theme, title);

        this.registry = registry;
        this.setting = setting;
        this.groups = groups;
    }

    protected final class Context {
        private WTable table;
        private String filterText = "";
        private GroupedSetSetting.Groups<T>.Group expanded = null;
        private GroupedSetSetting.Groups<T>.Group beingEdited = null;
        private IGroup<T, GroupedSetSetting.Groups<T>.Group> source;
        private Runnable reload;
    }

    private final Context ctx = new Context();

    private void createWidgets(WindowScreen root, Context ctx) {
        // Filter
        WTextBox filter = root.add(theme.textBox("")).minWidth(400).expandX().widget();
        filter.setFocused(true);

        ctx.table = root.add(theme.table()).expandX().widget();

        filter.action = () -> {
            ctx.filterText = filter.get().trim();

            ctx.table.clear();
            initTable(ctx);
        };

        initTable(ctx);
    }

    @Override
    public void initWidgets() {
        this.ctx.source = this.setting.get();
        this.ctx.reload = this::reload;
        createWidgets(this, this.ctx);
    }

    private void initTable(Context ctx) {

        List<ItemUnion> list = new ArrayList<>(groups.getAll().stream().map(ItemUnion::new).toList());

        registry.forEach((t) -> list.add(new ItemUnion(t)));

        // Left (all)
        WTable left = abc(ctx, list, true, t -> {
            addValue(ctx, t);

            if (t.t != null) {
                T v = getAdditionalValue(t.t);
                if (v != null) addValue(ctx, t);
            }
        });

        if (!left.cells.isEmpty()) ctx.table.add(theme.verticalSeparator()).expandWidgetY();

        list.clear();
        list.addAll(ctx.source.getGroups().stream().map(ItemUnion::new).toList());
        ctx.source.getImmediate().forEach((t) -> list.add(new ItemUnion(t)));

        // Right (selected)
        WTable right = abc(ctx, list, false, t -> {
            removeValue(ctx, t);

            if (t.t != null) {
                T v = getAdditionalValue(t.t);
                if (v != null) removeValue(ctx, t);
            }
        });

        postWidgets(left, right);
    }

    private WWidget groupLabel(GroupedSetSetting.Groups<T>.Group s) {
        Color color = entireGroupExcluded(s) ? new Color(200, 100, 10) : Color.ORANGE;
        if (s.showIcon.get()) return theme.itemWithLabel(s.icon.get().asItem().getDefaultStack(), "@"+s.name.get()).color(color);
        else return theme.label(" @"+s.name.get()).color(color);
    }

    private void addGroupTransferButton(Context ctx, WTable table,GroupedSetSetting.Groups<T>.Group g) {
        if (ctx.source.getImmediate().containsAll(g.getAllMatching(setting.getFilter()))) {
            table.add(theme.button(GuiRenderer.ARROWHEAD_DOUBLE.icon(180, Color.RED))).right().top().widget().action = () -> {
                ctx.source.removeAll(g.getAll());
                invalidateTable(ctx);
            };
        } else {
                table.add(theme.button(GuiRenderer.ARROWHEAD_DOUBLE.icon(Color.CYAN))).right().top().widget().action = () -> {
                ctx.source.addAll(g.getAll());
                invalidateTable(ctx);
            };
        }
    }

    private void addTransferButton(Context ctx, WTable table, T t) {
        if (ctx.source.getImmediate().contains(t)) {
            table.add(theme.button(GuiRenderer.ARROWHEAD.icon(180, Color.RED))).right().top().widget().action = () -> {
                ctx.source.remove(t);
                invalidateTable(ctx);
            };
        } else {
            table.add(theme.button(GuiRenderer.ARROWHEAD.icon(Color.ORANGE))).right().top().widget().action = () -> {
                ctx.source.add(t);
                invalidateTable(ctx);
            };
        }
    }

    private WTable abc(Context ctx, Iterable<ItemUnion> iterable, boolean isLeft, Consumer<ItemUnion> buttonAction) {
        // Create
        Cell<WTable> cell = ctx.table.add(theme.table()).top();
        WTable table = cell.widget();

        // Sort
        Predicate<ItemUnion> predicate = isLeft
            ? v -> Boolean.TRUE.equals(v.map(
                t -> this.includeValue(t) && !ctx.source.getImmediate().contains(t),
            s -> !entireGroupExcluded(s) && !ctx.source.getGroups().contains(s) && s != ctx.beingEdited))
            : v -> true;

        Iterable<ItemUnion> sorted = SortingHelper.sort(iterable, predicate, v -> v.map(this::getValueNames, s -> new String[]{"@"+s.name.get()}), ctx.filterText);

        sorted.forEach(v -> {

            WTable buttons = theme.table();

            if (v.s == null) {
                table.add(getValueWidget(v.t));
                table.add(buttons).right();
            } else {
                GroupedSetSetting.Groups<T>.Group s = v.s;

                WTable header = table.add(theme.table()).widget();

                boolean e = ctx.expanded == s;

                WButton expand = header.add(theme.button(GuiRenderer.TRIANGLE.icon(e ? 0 : -90))).widget();
                expand.action = () -> {
                    ctx.expanded = e ? null : s;
                    ctx.reload.run();
                };

                header.add(groupLabel(s));

                // Recursive editing is confusing
                if (ctx.beingEdited == null) {
                    WButton edit = buttons.add(theme.button(GuiRenderer.EDIT)).right().top().widget();
                    edit.action = () -> MeteorClient.mc.setScreen(new EditListGroupScreen(theme, s, () -> {
                        invalidateTable(ctx);
                        ctx.reload.run();
                    }));
                }

                addGroupTransferButton(ctx, buttons, s);

                table.add(buttons).right();

                if (e) {
                    for (GroupedSetSetting.Groups<T>.Group inc : s.getGroups()) {
                        table.row();

                        WTable label = theme.table();
                        label.add(theme.label("   -> "));
                        label.add(groupLabel(inc));
                        table.add(label);

                        addGroupTransferButton(ctx, table, inc);
                    }

                    Iterable<T> subitems = SortingHelper.sortWithPriority(s.getImmediate(), (t)->true, this::getValueNames, "", (T a, T b) -> includeValue(a) == includeValue(b) ? 0 : includeValue(a) ? -1 : 1);

                    subitems.forEach(t -> {
                        table.row();

                        WTable label = theme.table();
                        label.add(theme.label("   -> "));
                        label.add(getValueWidget(t));
                        table.add(label);

                        addTransferButton(ctx, table, t);
                    });
                }

            }

            WPressable button = buttons.add(isLeft ? theme.plus() : theme.minus()).expandCellX().right().top().widget();
            button.action = () -> buttonAction.accept(v);

            table.row();
        });

        if (!table.cells.isEmpty()) cell.expandX();

        return table;
    }

    private boolean entireGroupExcluded(GroupedSetSetting.Groups<T>.Group s) {
        return !s.anyMatch(this::includeValue);
    }

    protected void invalidateTable(Context ctx) {
        ctx.table.clear();
        initTable(ctx);
    }

    protected void addValue(Context ctx, ItemUnion value) {
        if (value.t != null) {
            ctx.source.add(value.t);
            setting.onChanged();
            invalidateTable(ctx);
        } else if (value.s != null) {
            ctx.source.add(value.s);
            setting.onChanged();
            invalidateTable(ctx);
        }
    }

    protected void removeValue(Context ctx, ItemUnion value) {
       if (value.t != null) {
            ctx.source.remove(value.t);
            setting.onChanged();
            invalidateTable(ctx);
        } else if (value.s != null) {
            ctx.source.remove(value.s);
            setting.onChanged();
            invalidateTable(ctx);
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

    public class EditListGroupScreen extends WindowScreen {

        private final GroupedSetSetting.Groups<T>.Group value;
        private final Runnable reload;

        private final Context ctx = new Context();

        private WContainer container;

        public EditListGroupScreen(GuiTheme theme, GroupedSetSetting.Groups<T>.Group value, Runnable reload) {
            super(theme, "Editing Group");

            this.value = value == null ? setting.createGroup("new-group") : value;
            this.reload = reload;
            this.value.builtin = false;

            GroupedSetSettingScreen.this.setting.set(GroupedSetSettingScreen.this.setting.get());
        }

        @Override
        public void initWidgets() {
            container = add(theme.verticalList()).expandX().minWidth(400).padTop(4).widget();
            container.add(theme.settings(value.settings)).expandX().padBottom(4);

            add(theme.horizontalSeparator("Contents")).expandX().padBottom(4);

            this.ctx.source = value;
            this.ctx.reload = this::reload;
            this.ctx.beingEdited = value;
            createWidgets(this, this.ctx);
        }

        @Override
        protected void onClosed() {
            this.reload.run();
        }

        @Override
        public void tick() {
            value.settings.tick(container, theme);
        }
    }
}
