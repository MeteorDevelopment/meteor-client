/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/** One-shot command: press its keybind to run a raw Baritone command. */
public class Command extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("Raw Baritone command without the prefix, e.g. \"farm 30\" or \"build house\".")
        .defaultValue("")
        .build()
    );

    public Command() {
        super(Categories.Baritone, "command", "Runs a raw Baritone command.");
    }

    @Override
    public void onActivate() {
        if (BaritoneUtil.check(this)) BaritoneUtils.runCommand(command.get());

        toggle();
    }
}
