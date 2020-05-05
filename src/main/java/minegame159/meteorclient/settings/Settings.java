package minegame159.meteorclient.settings;

import minegame159.meteorclient.gui.widgets.WTable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Settings implements Iterable<Setting<?>> {
    private SettingGroup defaultGroup;
    private final List<SettingGroup> groups = new ArrayList<>(1);

    private WTable table;

    public Setting<?> get(String name) {
        for (Setting<?> setting : this) {
            if (name.equalsIgnoreCase(setting.name)) return setting;
        }

        return null;
    }

    public int sizeGroups() {
        return groups.size();
    }

    public SettingGroup getDefaultGroup() {
        if (defaultGroup == null) {
            defaultGroup = new SettingGroup(this, "General", null, null, false, null);
            groups.add(defaultGroup);
        }
        return defaultGroup;
    }

    public SettingGroup createGroup(String name, String enabledName, String enabledDescription, boolean enabled, EnabledChangedListener enabledChangedListener) {
        SettingGroup group = new SettingGroup(this, name, enabledName, enabledDescription, enabled, enabledChangedListener);
        groups.add(group);
        return group;
    }
    public SettingGroup createGroup(String name, String enabledName, String enabledDescription, boolean enabled) {
        return createGroup(name, enabledName, enabledDescription, enabled, null);
    }
    public SettingGroup createGroup(String name) {
        return createGroup(name, null, null, false, null);
    }

    public WTable createTable(boolean activate) {
        table = new WTable();

        for (Setting<?> setting : this) {
            if (activate) setting.onActivated();
            setting.resetWidget();
        }

        for (SettingGroup group : groups) {
            group.fillTable(table);
        }

        return table;
    }
    public WTable createTable() {
        return createTable(true);
    }

    void refreshTable() {
        if (table != null) {
            table.clear();

            for (SettingGroup group : groups) {
                group.fillTable(table);
            }
        }
    }

    @Override
    public Iterator<Setting<?>> iterator() {
        return new SettingsIterator();
    }

    private class SettingsIterator implements Iterator<Setting<?>> {
        private int groupI = 0;
        private Iterator<Setting<?>> groupIterator;

        public SettingsIterator() {
            if (groups.size() > 0) groupIterator = groups.get(groupI++).iterator();
        }

        @Override
        public boolean hasNext() {
            return groupI < groups.size() || (groupIterator != null && groupIterator.hasNext());
        }

        @Override
        public Setting<?> next() {
            if (groupIterator.hasNext()) return groupIterator.next();

            while (true) {
                groupIterator = groups.get(groupI++).iterator();

                if (groupIterator.hasNext()) return groupIterator.next();
            }
        }
    }
}
