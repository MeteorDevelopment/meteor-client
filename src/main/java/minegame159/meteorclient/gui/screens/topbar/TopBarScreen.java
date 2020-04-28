package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.screens.ModuleScreen;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WTopBar;
import minegame159.meteorclient.gui.widgets.WWindow;
import minegame159.meteorclient.settings.Setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TopBarScreen extends WidgetScreen {
    public final TopBarType type;

    public final Map<String, List<Setting<?>>> settingGroups = new LinkedHashMap<>(1);
    public final List<Setting<?>> settings = new ArrayList<>(1);

    public TopBarScreen(TopBarType type) {
        super(type.toString());
        this.type = type;

        add(new WTopBar()).centerX();
    }

    public <T> Setting<T> addSetting(Setting<T> setting) {
        settings.add(setting);
        List<Setting<?>> group = settingGroups.computeIfAbsent(setting.group == null ? "Other" : setting.group, s -> new ArrayList<>(1));
        group.add(setting);
        return setting;
    }

    public void createSettingsWindow() {
        WWindow window = add(new WWindow(title, true)).centerXY().getWidget();

        // Settings
        if (settingGroups.size() > 0) {
            WTable table = window.add(new WTable()).fillX().expandX().getWidget();
            for (String group : settingGroups.keySet()) {
                if (settingGroups.size() > 1) {
                    table.add(new WHorizontalSeparator(group)).fillX().expandX();
                    table.row();
                }

                for (Setting<?> setting : settingGroups.get(group)) {
                    if (setting.isVisible()) {
                        ModuleScreen.generateSettingToGrid(table, setting);
                    }
                }
            }
        }
    }
}
