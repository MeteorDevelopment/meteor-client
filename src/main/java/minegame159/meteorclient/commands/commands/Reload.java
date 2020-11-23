/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.commands.Command;

public class Reload extends Command {
    public Reload() {
        super("reload", "Reloads config, modules, friends, macros and accounts.");
    }

    @Override
    public void run(String[] args) {
        MeteorClient.INSTANCE.load();
    }
}
