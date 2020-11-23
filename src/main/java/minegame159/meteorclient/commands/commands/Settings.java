/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;

public class Settings extends Command {
    public Settings() {
        super("settings", "Displays all settings of specified module.");
    }

    @Override
    public void run(String[] args) {
        Module module = Utils.tryToGetModule(args);
        if (module == null) return;

        Chat.info("(highlight)%s(default):", module.title);
        for (SettingGroup sg : module.settings) {
            for (Setting<?> setting : sg) {
                Chat.info("  Usage of (highlight)%s (default)(%s) is (highlight)%s(default).", setting.name, setting.get().toString(), setting.getUsage());
            }
        }
    }
}
