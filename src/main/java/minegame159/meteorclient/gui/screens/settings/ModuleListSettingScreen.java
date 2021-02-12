/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.gui.screens.settings;

import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.settings.Setting;

import java.util.List;

public class ModuleListSettingScreen extends LeftRightListSettingScreen<Module> {
    public ModuleListSettingScreen(Setting<List<Module>> setting) {
        super("Select Modules", setting, Modules.REGISTRY);
    }

    @Override
    protected WWidget getValueWidget(Module value) {
        return new WLabel(value.title);
    }

    @Override
    protected String getValueName(Module value) {
        return value.title;
    }
}
