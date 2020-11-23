/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;

public class ClearChat extends Command {
    public ClearChat() {
        super("clear-chat", "Clears your chat.");
    }

    @Override
    public void run(String[] args) {
        MC.inGameHud.getChatHud().clear(false);
    }
}
