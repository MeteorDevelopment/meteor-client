package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.gui.widgets.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingGroup implements Iterable<Setting<?>> {
    private static final EmptyIterator EMPTY_ITERATOR = new EmptyIterator();

    public final String name;

    private final Settings parent;
    private final Setting<Boolean> enabledSetting;
    final List<Setting<?>> settings = new ArrayList<>(1);
    private final EnabledChangedListener enabledChangedListener;

    private SettingGroup disabledGroup;

    SettingGroup(Settings parent, String name, String enabledName, String enabledDescription, boolean enabled, EnabledChangedListener enabledChangedListener) {
        this.parent = parent;
        this.name = name;

        if (enabledName != null) {
            enabledSetting = add(new BoolSetting.Builder()
                    .name(enabledName)
                    .description(enabledDescription)
                    .defaultValue(enabled)
                    .onChanged(aBoolean -> {
                        parent.refreshTable();
                        if (enabledChangedListener != null) enabledChangedListener.onEnabledChanged(this);
                    })
                    .build());
        } else enabledSetting = null;

        this.enabledChangedListener = enabledChangedListener;
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean isEnabled() {
        return enabledSetting == null || enabledSetting.get();
    }

    public void setEnabled(boolean enabled) {
        if (enabledSetting != null) {
            enabledSetting.set(enabled);
        }
    }

    public <T> Setting<T> add(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public boolean hasDisabledGroup() {
        return disabledGroup != null;
    }

    public SettingGroup getDisabledGroup() {
        if (disabledGroup == null) disabledGroup = new SettingGroup(parent, null, null, null, false, null);
        return disabledGroup;
    }

    public void fillTable(WTable table) {
        if (hasName()) {
            table.add(new WHorizontalSeparator(name)).fillX().expandX();
            table.row();
        }

        if (enabledSetting != null) fillTable(table, enabledSetting);

        if (isEnabled()) {
            for (Setting<?> setting : settings) {
                if (setting != enabledSetting) fillTable(table, setting);
            }
        } else if (hasDisabledGroup()) {
            getDisabledGroup().fillTable(table);
        }
    }

    private void fillTable(WTable table, Setting<?> setting) {
        WLabel label = table.add(new WLabel(setting.title)).getWidget();
        label.tooltip = setting.description;

        WWidget widget = table.add(setting.widget).getWidget();
        widget.tooltip = setting.description;

        WButton reset = table.add(new WButton(GuiRenderer.TEX_RESET)).fillX().right().getWidget();
        reset.tooltip = "Reset";
        reset.action = button -> setting.reset();

        table.row();
    }

    @Override
    public Iterator<Setting<?>> iterator() {
        return new SettingGroupIterator();
    }

    private class SettingGroupIterator implements Iterator<Setting<?>> {
        private final Iterator<Setting<?>> iter1 = settings.iterator();
        private final Iterator<Setting<?>> iter2 = disabledGroup != null ? disabledGroup.iterator() : EMPTY_ITERATOR;

        @Override
        public boolean hasNext() {
            return iter1.hasNext() || iter2.hasNext();
        }

        @Override
        public Setting<?> next() {
            if (iter1.hasNext()) return iter1.next();
            return iter2.next();
        }
    }

    private static class EmptyIterator implements Iterator<Setting<?>> {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Setting<?> next() {
            return null;
        }
    }
}
