/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;

import java.util.ArrayList;
import java.util.List;

public class Panic extends Command {
    public Panic() {
        super("panic", "Disables all modules.");
    }

    @Override
    public void run(String[] args) {
        List<ToggleModule> active = new ArrayList<>(ModuleManager.INSTANCE.getActive());
        for (ToggleModule module : active) module.toggle();
    }
}
