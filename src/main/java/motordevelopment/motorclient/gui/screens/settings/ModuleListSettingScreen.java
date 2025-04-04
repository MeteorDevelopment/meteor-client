/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.gui.screens.settings;

import motordevelopment.motorclient.gui.GuiTheme;
import motordevelopment.motorclient.gui.widgets.WWidget;
import motordevelopment.motorclient.settings.Setting;
import motordevelopment.motorclient.systems.modules.Module;
import motordevelopment.motorclient.systems.modules.Modules;

import java.util.List;

public class ModuleListSettingScreen extends RegistryListSettingScreen<Module> {
    public ModuleListSettingScreen(GuiTheme theme, Setting<List<Module>> setting) {
        super(theme, "Select Modules", setting, setting.get(), Modules.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(Module value) {
        return theme.label(getValueName(value));
    }

    @Override
    protected String getValueName(Module value) {
        return value.title;
    }
}
