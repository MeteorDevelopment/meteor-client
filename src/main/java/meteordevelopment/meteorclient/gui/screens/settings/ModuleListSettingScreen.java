/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

import java.util.List;

public class ModuleListSettingScreen extends LeftRightListSettingScreen<Module> {
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
