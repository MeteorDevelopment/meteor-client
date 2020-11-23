/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.utils.Chat;

public class ResetGui extends Command {
    public ResetGui() {
        super("reset-gui", "Resets gui positions.");
    }

    @Override
    public void run(String[] args) {
        Config.INSTANCE.guiConfig.clearWindowConfigs();
        Chat.info("Gui positions has been reset.");
    }
}
