/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import baritone.api.BaritoneAPI;
import minegame159.meteorclient.commands.Command;

public class Baritone extends Command {
    public Baritone() {
        super("b", "Baritone.");
    }

    @Override
    public void run(String[] args) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(String.join(" ", args));
    }
}
