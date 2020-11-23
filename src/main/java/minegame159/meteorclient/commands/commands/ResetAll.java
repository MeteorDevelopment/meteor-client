/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;

public class ResetAll extends Command {
    public ResetAll() {
        super("reset-all", "Resets all modules oldsettings.");
    }

    @Override
    public void run(String[] args) {
        for (Module module : ModuleManager.INSTANCE.getAll()) {
            for (SettingGroup sg : module.settings) {
                for (Setting<?> setting : sg) setting.reset();
            }
        }
    }
}
