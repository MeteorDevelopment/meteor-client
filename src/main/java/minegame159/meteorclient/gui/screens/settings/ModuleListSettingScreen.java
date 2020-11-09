package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.Setting;

import java.util.List;

public class ModuleListSettingScreen extends LeftRightListSettingScreen<ToggleModule> {
    public ModuleListSettingScreen(Setting<List<ToggleModule>> setting) {
        super("Select Modules", setting, ModuleManager.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(ToggleModule value) {
        return new WLabel(value.title);
    }

    @Override
    protected String getValueName(ToggleModule value) {
        return value.title;
    }
}
