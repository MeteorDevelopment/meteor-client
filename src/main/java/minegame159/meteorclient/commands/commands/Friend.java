/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import minegame159.meteorclient.commands.Command;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.utils.Chat;

public class Friend extends Command {
    public Friend() {
        super("friend", "Manages friends.");
    }

    @Override
    public void run(String[] args) {
        if (args.length == 0) {
            sendErrorWrongSubcommand();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add": {
                if (args.length < 2) {
                    sendErrorEnterName();
                    return;
                }

                String name = "";
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) name += " ";
                    name += args[i];
                }

                if (FriendManager.INSTANCE.add(new minegame159.meteorclient.friends.Friend(name))) {
                    Chat.info("Added (highlight)%s (default)to friends.", name);
                }

                break;
            }
            case "remove": {
                if (args.length < 2) {
                    sendErrorEnterName();
                    return;
                }

                String name = "";
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) name += " ";
                    name += args[i];
                }

                if (FriendManager.INSTANCE.remove(new minegame159.meteorclient.friends.Friend(name))) {
                    Chat.info("Removed (highlight)%s (default)from friends.", name);
                }

                break;
            }
            case "list": {
                Chat.info("You have (highlight)%d (default)friends:", FriendManager.INSTANCE.count());

                for (minegame159.meteorclient.friends.Friend friend : FriendManager.INSTANCE) {
                    Chat.info(" - (highlight)%s", friend.name);
                }

                break;
            }
            default: sendErrorWrongSubcommand();
        }
    }

    private void sendErrorWrongSubcommand() {
        Chat.error("Wrong sub-command. Use (highlight)add(default), (highlight)remove (default)or (highlight)list(default).");
    }

    private void sendErrorEnterName() {
        Chat.error("Enter name of your friend.");
    }
}
